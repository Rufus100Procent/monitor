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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class PollingEngine {

    private static final Logger log = LoggerFactory.getLogger(PollingEngine.class);
    private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");
    private static final String HTTP_SERVER_REQUESTS = "http.server.requests";

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

    @PostConstruct
    public void init() {
        log.info("PollingEngine starting");
        Flux.interval(Duration.ZERO, Duration.ofSeconds(2))
                .flatMap(_ -> syncWithDatabase()
                        .onErrorResume(error -> {
                            log.warn("Sync failed: {}", error.getMessage());
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
                    activeServers.forEach(server -> {
                        if (!activePollers.containsKey(server.getId())) {
                            log.info("Starting poller for server={} interval={}s",
                                    server.getName(), server.getPollIntervalSeconds());
                            startPolling(server);
                        }
                    });

                    Set<UUID> activeIds = activeServers.stream()
                            .map(ServerRegister::getId)
                            .collect(Collectors.toSet());

                    activePollers.keySet().forEach(id -> {
                        if (!activeIds.contains(id)) {
                            log.info("Stopping poller for serverId={} (paused or deleted)", id);
                            stopPolling(id);
                        }
                    });
                })
                .then();
    }

    private void startPolling(ServerRegister server) {
        stopPolling(server.getId());

        Disposable disposable = Flux.interval(Duration.ZERO, Duration.ofSeconds(server.getPollIntervalSeconds()))
                .flatMap(_ -> poll(server))
                .subscribe(
                        _ -> {},
                        error -> log.error("Poller crashed for server={} reason={} — poller stopped",
                                server.getName(), error.getMessage())
                );

        activePollers.put(server.getId(), disposable);
    }

    private void stopPolling(UUID serverId) {
        Disposable existing = activePollers.remove(serverId);
        if (existing != null && !existing.isDisposed()) {
            existing.dispose();
        }
    }

    private Mono<ServerSnapshot> poll(ServerRegister server) {
        String baseUrl = server.getBaseUrl();
        String actuatorPath = server.getActuatorPath();

        return Mono.zip(
                        actuatorClient.fetchHealth(baseUrl, actuatorPath),
                        actuatorClient.fetchHealthAndDisk(baseUrl, actuatorPath),
                        actuatorClient.fetchAppName(baseUrl, actuatorPath)
                                .map(name -> "unknown".equals(name) ? server.getName() : name),
                        actuatorClient.fetchAppVersion(baseUrl, actuatorPath),
                        actuatorClient.fetchMetricValue(baseUrl, actuatorPath, "jvm.memory.used"),
                        actuatorClient.fetchMetricValue(baseUrl, actuatorPath, "jvm.memory.max"),
                        actuatorClient.fetchMetricValue(baseUrl, actuatorPath, "system.cpu.usage")
                )
                .flatMap(tuple -> {
                    String health     = tuple.getT1();
                    long[] disk       = tuple.getT2();
                    String appName    = tuple.getT3();
                    String appVersion = tuple.getT4();
                    Double memoryUsed = tuple.getT5();
                    Double memoryMax  = tuple.getT6();
                    Double cpu        = tuple.getT7();

                    return Mono.zip(
                                    actuatorClient.fetchMetricValue(baseUrl, actuatorPath, "process.uptime"),
                                    actuatorClient.fetchMetricValue(baseUrl, actuatorPath, "system.load.average.1m"),
                                    actuatorClient.fetchMetricValue(baseUrl, actuatorPath, "jvm.threads.live"),
                                    actuatorClient.fetchMetricValue(baseUrl, actuatorPath, "jvm.gc.overhead"),
                                    actuatorClient.fetchMetricStatistic(baseUrl, actuatorPath, HTTP_SERVER_REQUESTS, "COUNT"),
                                    actuatorClient.fetchMetricStatistic(baseUrl, actuatorPath, HTTP_SERVER_REQUESTS, "TOTAL_TIME"),
                                    actuatorClient.fetchMetricStatistic(baseUrl, actuatorPath, HTTP_SERVER_REQUESTS, "MAX")
                            )
                            .flatMap(tuple2 -> {
                                Double uptime     = tuple2.getT1();
                                Double systemLoad = tuple2.getT2();
                                Double threads    = tuple2.getT3();
                                Double gcOverhead = tuple2.getT4();
                                Double totalCount = tuple2.getT5();
                                Double totalTime  = tuple2.getT6();
                                Double maxTime    = tuple2.getT7();

                                return Mono.zip(
                                                actuatorClient.fetchMetricValueByTag(baseUrl, actuatorPath, HTTP_SERVER_REQUESTS, "SUCCESS"),
                                                actuatorClient.fetchMetricValueByTag(baseUrl, actuatorPath, HTTP_SERVER_REQUESTS, "CLIENT_ERROR"),
                                                actuatorClient.fetchMetricValueByTag(baseUrl, actuatorPath, HTTP_SERVER_REQUESTS, "SERVER_ERROR")
                                        )
                                        .flatMap(httpTuple -> {
                                            Double successCount = httpTuple.getT1();
                                            Double clientError  = httpTuple.getT2();
                                            Double serverError  = httpTuple.getT3();

                                            double avgMs = totalCount > 0 ? (totalTime / totalCount) * 1000 : 0.0;
                                            double maxMs = maxTime * 1000;

                                            ServerSnapshot snapshot = new ServerSnapshot();
                                            snapshot.setId(UUID.randomUUID());
                                            snapshot.setServerId(server.getId());
                                            snapshot.setPolledAt(Instant.now());
                                            snapshot.setMonitorTimezone(STOCKHOLM.toString());
                                            snapshot.setMonitorLocalTime(LocalDateTime.now(STOCKHOLM).toString());
                                            snapshot.setHealthStatus(health);
                                            snapshot.setAppName(appName);
                                            snapshot.setAppVersion(appVersion);
                                            snapshot.setMemoryUsedBytes(memoryUsed.longValue());
                                            snapshot.setMemoryMaxBytes(memoryMax.longValue());
                                            snapshot.setCpuUsage(cpu);
                                            snapshot.setUptimeSeconds(uptime);
                                            snapshot.setSystemLoad(systemLoad);
                                            snapshot.setJvmThreadsLive(threads.longValue());
                                            snapshot.setGcOverhead(gcOverhead);
                                            snapshot.setDiskTotalBytes(disk[0]);
                                            snapshot.setDiskFreeBytes(disk[1]);
                                            snapshot.setHttpRequestCount(totalCount.longValue());
                                            snapshot.setHttpAvgMs(avgMs);
                                            snapshot.setHttpMaxMs(maxMs);
                                            snapshot.setHttp2xxCount(successCount.longValue());
                                            snapshot.setHttp4xxCount(clientError.longValue());
                                            snapshot.setHttp5xxCount(serverError.longValue());
                                            snapshot.setPollSuccess(!"DOWN".equals(health));

                                            return serverSnapshotService.saveSnapshot(snapshot)
                                                    .flatMap(saved -> serverRegisterService
                                                            .updateAfterPoll(server, health)
                                                            .thenReturn(saved));
                                        });
                            });
                })
                .onErrorResume(ex -> {
                    log.error("Poll failed for server={} reason={}", server.getName(), ex.getMessage());
                    return serverSnapshotService.saveFailedSnapshot(server, ex.getMessage());
                });
    }
}