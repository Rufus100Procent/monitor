package com.rufus100procent.monitor.service;

import com.rufus100procent.monitor.modal.ServerRegister;
import com.rufus100procent.monitor.modal.ServerSnapshot;
import com.rufus100procent.monitor.repo.ServerRegisterRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple6;
import reactor.util.function.Tuple7;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class PollingEngine {

    private static final Logger log = LoggerFactory.getLogger(PollingEngine.class);
    private static final String HTTP_REQUESTS = "http.server.requests";

    private final ServerRegisterRepository serverRegisterRepository;
    private final ServerSnapshotService serverSnapshotService;
    private final ServerRegisterService serverRegisterService;
    private final ActuatorClient actuatorClient;

    private final Map<UUID, Disposable> activePollers = new ConcurrentHashMap<>();

    public PollingEngine(ServerRegisterRepository serverRegisterRepository,
                         ServerSnapshotService serverSnapshotService,
                         ServerRegisterService serverRegisterService,
                         ActuatorClient actuatorClient) {
        this.serverRegisterRepository = serverRegisterRepository;
        this.serverSnapshotService = serverSnapshotService;
        this.serverRegisterService = serverRegisterService;
        this.actuatorClient = actuatorClient;
    }

//    @PostConstruct
    public void init() {
        log.info("PollingEngine starting");
        Flux.interval(Duration.ZERO, Duration.ofSeconds(2))
                .flatMap(_ -> syncWithDatabase()
                        .onErrorResume(error -> {
                            log.warn("Sync failed {}", error.getMessage());
                            return Mono.empty();
                        })
                )
                .subscribe(
                        null,
                        error -> log.error("PollingEngine fatal sync error: {}", error.getMessage())
                );
    }

    private Mono<Void> syncWithDatabase() {
        return serverRegisterRepository.findAllByPauseFalse()
                .collectList()
                .doOnNext(activeServers -> {
                    // start new pollers
                    activeServers.forEach(server -> {
                        if (!activePollers.containsKey(server.getId())) {
                            log.info("Starting poller for server={} interval={}s",
                                    server.getAppName(), server.getPollIntervalSeconds());
                            startPolling(server);
                        }
                    });
                    Set<UUID> activeIds = activeServers.stream()
                            .map(ServerRegister::getId)
                            .collect(Collectors.toSet());

                    activePollers.keySet().forEach(id -> {
                        if (!activeIds.contains(id)) {
                            log.info("Stopping poller for serverId={}", id);
                            stopPolling(id);
                        }
                    });
                })
                .then();
    }

    private void startPolling(ServerRegister server) {
        stopPolling(server.getId());
        Disposable disposable = Flux.interval(Duration.ZERO, Duration.ofSeconds(server.getPollIntervalSeconds()))
                .flatMap(_ -> collectMetrics(server)
                        .flatMap(snapshot -> saveSnapshot(snapshot)
                                .flatMap(saved -> updateRegister(server, snapshot.getHealthStatus())
                                        .thenReturn(saved)))
                        .onErrorResume(ex -> {
                            log.error("Poll failed for server={} reason={}", server.getAppName(), ex.getMessage());
                            return saveFailedSnapshot(server);
                        })
                )
                .subscribe(
                        _ -> {},
                        error -> log.error("Poller crashed for server={} reason={}", server.getAppName(), error.getMessage())
                );
        activePollers.put(server.getId(), disposable);
    }

    private void stopPolling(UUID serverId) {
        Disposable existing = activePollers.remove(serverId);
        if (existing != null && !existing.isDisposed()) {
            existing.dispose();
        }
    }

    // metrics in parallel
    private Mono<ServerSnapshot> collectMetrics(ServerRegister server) {
        String base = server.getBaseUrl();
        String path = server.getActuatorPath();

        return Mono.zip(
                actuatorClient.fetchHealth(base, path),
                actuatorClient.fetchAppVersion(base, path),
                actuatorClient.fetchMetric(base, path, "jvm.memory.used"),
                actuatorClient.fetchMetric(base, path, "system.cpu.usage"),
                actuatorClient.fetchMetric(base, path, "system.load.average.1m"),
                actuatorClient.fetchMetric(base, path, "process.uptime"),
                actuatorClient.fetchMetric(base, path, "jvm.threads.live")
        ).flatMap(t -> Mono.zip(
                actuatorClient.fetchMetric(base, path, "jvm.gc.overhead"),
                actuatorClient.fetchMetricStatistic(base, path, HTTP_REQUESTS, "COUNT"),
                actuatorClient.fetchMetricStatistic(base, path, HTTP_REQUESTS, "TOTAL_TIME"),
                actuatorClient.fetchMetricByOutcome(base, path, HTTP_REQUESTS, "SUCCESS"),
                actuatorClient.fetchMetricByOutcome(base, path, HTTP_REQUESTS, "REDIRECTION"),
                actuatorClient.fetchMetricByOutcome(base, path, HTTP_REQUESTS, "CLIENT_ERROR"),
                actuatorClient.fetchMetricByOutcome(base, path, HTTP_REQUESTS, "SERVER_ERROR")
        ).map(t2 -> buildSnapshot(server, t, t2)));
    }

    // build snapshot from collected data
    private ServerSnapshot buildSnapshot(
            ServerRegister server,
            Tuple7<ActuatorClient.HealthResult, String, Double, Double, Double, Double, Double> t,
            Tuple7<Double, Double, Double, Double, Double, Double, Double> t2) {

        ActuatorClient.HealthResult health = t.getT1();
        double count = t2.getT2();

        ServerSnapshot snapshot = new ServerSnapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setServerId(server.getId());
        snapshot.setPolledAt(Instant.now());
        snapshot.setHealthStatus(health.status());
        snapshot.setMemoryUsedBytes(t.getT3().longValue());
        snapshot.setCpuUsage(t.getT4());
        snapshot.setSystemLoad(t.getT5());
        snapshot.setUptimeSeconds(t.getT6());
        snapshot.setJvmThreadsLive(t.getT7().longValue());
        snapshot.setGcOverhead(t2.getT1());
        snapshot.setHttpRequestCount((long) count);
        snapshot.setHttpAvgMs(count > 0 ? (t2.getT3() / count) * 1000 : 0.0);
        snapshot.setHttp2xxCount(t2.getT4().longValue());
        snapshot.setHttp4xxCount(t2.getT5().longValue());
        snapshot.setHttp5xxCount(t2.getT6().longValue());
        snapshot.setHttp3xxCount(t2.getT7().longValue());
        snapshot.setDiskTotalBytes(health.diskTotal());
        snapshot.setDiskFreeBytes(health.diskFree());
        snapshot.setPollSuccess(!"DOWN".equals(health.status()));
        return snapshot;
    }

    private Mono<ServerSnapshot> saveSnapshot(ServerSnapshot snapshot) {
        return serverSnapshotService.saveSnapshot(snapshot);
    }

    private Mono<Void> updateRegister(ServerRegister server, String healthStatus) {
        return serverRegisterService.updateAfterPoll(server, healthStatus);
    }

    private Mono<ServerSnapshot> saveFailedSnapshot(ServerRegister server) {
        return serverSnapshotService.saveFailedSnapshot(server);
    }
}