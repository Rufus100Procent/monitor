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
    private final MonitorUserService monitorUserService;

    private final Map<UUID, Disposable> activePollers = new ConcurrentHashMap<>();

    public PollingEngine(ServerRegisterRepository serverRegisterRepository,
                         ServerSnapshotService serverSnapshotService,
                         ServerRegisterService serverRegisterService,
                         ActuatorClient actuatorClient, MonitorUserService monitorUserService) {
        this.serverRegisterRepository = serverRegisterRepository;
        this.serverSnapshotService = serverSnapshotService;
        this.serverRegisterService = serverRegisterService;
        this.actuatorClient = actuatorClient;
        this.monitorUserService = monitorUserService;
    }

    @PostConstruct
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
                .flatMap(server -> monitorUserService.resolveUserSecret(server.getUserId())
                        .map(secret -> {
                            if (!activePollers.containsKey(server.getId())) {
                                log.info("Starting poller for server={} interval={}s",
                                        server.getAppName(), server.getPollIntervalSeconds());
                                startPolling(server, secret);
                            }
                            return server;
                        }))
                .collectList()
                .doOnNext(activeServers -> {
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

    private void startPolling(ServerRegister server, String secret) {
        stopPolling(server.getId());
        Disposable disposable = Flux.interval(Duration.ZERO, Duration.ofSeconds(server.getPollIntervalSeconds()))
                .flatMap(_ -> collectMetrics(server, secret)
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
    private Mono<ServerSnapshot> collectMetrics(ServerRegister server, String secret) {
        String base = server.getBaseUrl();
        String path = server.getActuatorPath();

        return Mono.zip(
                actuatorClient.fetchHealth(base, path, secret),
                actuatorClient.fetchAppVersion(base, path, secret),
                actuatorClient.fetchMetric(base, path, "jvm.memory.used", secret),
                actuatorClient.fetchMetric(base, path, "system.cpu.usage", secret),
                actuatorClient.fetchMetric(base, path, "system.load.average.1m", secret),
                actuatorClient.fetchMetric(base, path, "process.uptime", secret),
                actuatorClient.fetchMetric(base, path, "jvm.threads.live", secret)
        ).flatMap(t -> Mono.zip(
                actuatorClient.fetchMetric(base, path, "jvm.gc.overhead", secret),
                actuatorClient.fetchMetricStatistic(base, path, HTTP_REQUESTS, "COUNT", secret),
                actuatorClient.fetchMetricStatistic(base, path, HTTP_REQUESTS, "TOTAL_TIME", secret),
                actuatorClient.fetchMetricByOutcome(base, path, HTTP_REQUESTS, "SUCCESS", secret),
                actuatorClient.fetchMetricByOutcome(base, path, HTTP_REQUESTS, "REDIRECTION", secret),
                actuatorClient.fetchMetricByOutcome(base, path, HTTP_REQUESTS, "CLIENT_ERROR", secret),
                actuatorClient.fetchMetricByOutcome(base, path, HTTP_REQUESTS, "SERVER_ERROR", secret)
        ).flatMap(t2 -> serverSnapshotService.findLatestRaw(server.getId())
                .map(prev -> buildSnapshot(server, t, t2, prev))
                .switchIfEmpty(Mono.fromCallable(() -> buildSnapshot(server, t, t2, null)))
        ));
    }

    private ServerSnapshot buildSnapshot(
            ServerRegister server,
            Tuple7<ActuatorClient.HealthResult, String, Double, Double, Double, Double, Double> t,
            Tuple7<Double, Double, Double, Double, Double, Double, Double> t2,
            ServerSnapshot prev) {

        ActuatorClient.HealthResult health = t.getT1();

        double cumulativeCount = t2.getT2();
        double cumulativeTime  = t2.getT3();
        double cumulative2xx   = t2.getT4();
        double cumulative3xx   = t2.getT5();
        double cumulative4xx   = t2.getT6();
        double cumulative5xx   = t2.getT7();

        // detect redeploy — app restarted and counters reset back to zero
        boolean appRestarted = prev != null && cumulativeCount < prev.getHttpRequestCount();

        // requests that happened in this poll window only (since last snapshot)
        long requestsThisPoll;
        long requests2xxThisPoll;
        long requests3xxThisPoll;
        long requests4xxThisPoll;
        long requests5xxThisPoll;
        double totalTimeThisPoll;

        if (prev == null || appRestarted) {
            // first snapshot ever, or app just restarted
            // no previous baseline to subtract from — use current cumulative as starting point
            requestsThisPoll      = (long) cumulativeCount;
            totalTimeThisPoll     = cumulativeTime;
            requests2xxThisPoll   = (long) cumulative2xx;
            requests3xxThisPoll   = (long) cumulative3xx;
            requests4xxThisPoll   = (long) cumulative4xx;
            requests5xxThisPoll   = (long) cumulative5xx;
        } else {
            // normal poll — subtract what we already counted last time
            // result = only the requests that arrived since the previous snapshot
            requestsThisPoll    = Math.max(0, (long) cumulativeCount - prev.getHttpRequestCount());
            totalTimeThisPoll   = Math.max(0, cumulativeTime - (prev.getHttpAvgMs() * prev.getHttpRequestCount() / 1000.0));
            requests2xxThisPoll = Math.max(0, (long) cumulative2xx - prev.getHttp2xxCount());
            requests3xxThisPoll = Math.max(0, (long) cumulative3xx - prev.getHttp3xxCount());
            requests4xxThisPoll = Math.max(0, (long) cumulative4xx - prev.getHttp4xxCount());
            requests5xxThisPoll = Math.max(0, (long) cumulative5xx - prev.getHttp5xxCount());
        }

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
        snapshot.setHttpRequestCount(requestsThisPoll);
        snapshot.setHttpAvgMs(requestsThisPoll > 0 ? (totalTimeThisPoll / requestsThisPoll) * 1000 : 0.0);
        snapshot.setHttp2xxCount(requests2xxThisPoll);
        snapshot.setHttp3xxCount(requests3xxThisPoll);
        snapshot.setHttp4xxCount(requests4xxThisPoll);
        snapshot.setHttp5xxCount(requests5xxThisPoll);
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