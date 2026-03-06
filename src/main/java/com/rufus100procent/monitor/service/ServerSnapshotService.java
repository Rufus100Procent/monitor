package com.rufus100procent.monitor.service;

import com.rufus100procent.monitor.dto.ServerSnapshotDto;
import com.rufus100procent.monitor.modal.ServerRegister;
import com.rufus100procent.monitor.modal.ServerSnapshot;
import com.rufus100procent.monitor.repo.ServerRegisterRepository;
import com.rufus100procent.monitor.repo.ServerSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
public class ServerSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(ServerSnapshotService.class);
    private static final int DEFAULT_LIMIT = 50;
    private static final String DEFAULT_SORT = "DESC";

    private final ServerSnapshotRepository snapshotRepository;
    private final ServerRegisterRepository serverRegisterRepository;

    public ServerSnapshotService(ServerSnapshotRepository snapshotRepository,
                                 ServerRegisterRepository serverRegisterRepository) {
        this.snapshotRepository = snapshotRepository;
        this.serverRegisterRepository = serverRegisterRepository;
    }

    public Mono<ServerSnapshot> saveSnapshot(ServerSnapshot snapshot) {
        return snapshotRepository.save(snapshot)
                .doOnSuccess(s -> {
                    assert s != null;
                    log.info("Snapshot saved id={} server={} health={}",
                            s.getId(), s.getServerId(), s.getHealthStatus());
                })
                .doOnError(e -> log.error("Failed to save snapshot for serverId={} reason={}",
                        snapshot.getServerId(), e.getMessage()));
    }

    public Mono<ServerSnapshot> saveFailedSnapshot(ServerRegister server) {
        log.warn("Saving failed snapshot for server={}", server.getAppName());
        ServerSnapshot snapshot = new ServerSnapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setServerId(server.getId());
        snapshot.setPolledAt(Instant.now());
        snapshot.setHealthStatus("DOWN");
        snapshot.setPollSuccess(false);
        return snapshotRepository.save(snapshot)
                .doOnError(e -> log.error("Failed to save failed-snapshot for server={} reason={}",
                        server.getAppName(), e.getMessage()));
    }

    public Flux<ServerSnapshotDto> getSnapshots(UUID serverId, Instant from, Instant to,
                                                String sort, int limit) {
        String resolvedSort  = (sort != null && sort.equalsIgnoreCase("ASC")) ? "ASC" : DEFAULT_SORT;
        int resolvedLimit    = (limit > 0 && limit <= 1000) ? limit : DEFAULT_LIMIT;
        Instant resolvedFrom = from != null ? from : Instant.EPOCH;
        Instant resolvedTo   = to != null ? to : Instant.now();

        return serverRegisterRepository.findById(serverId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Server not found with id: " + serverId)))
                .flatMapMany(_ -> snapshotRepository.findSnapshots(
                        serverId, resolvedFrom, resolvedTo, resolvedSort, resolvedLimit))
                .map(this::toDto);
    }

    public Mono<ServerSnapshotDto> getLatest(UUID serverId) {
        return serverRegisterRepository.findById(serverId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Server not found with id: " + serverId)))
                .flatMap(_ -> snapshotRepository.findTopByServerIdOrderByPolledAtDesc(serverId))
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "No snapshots found for server: " + serverId)))
                .map(this::toDto);
    }

    private ServerSnapshotDto toDto(ServerSnapshot snapshot) {
        ServerSnapshotDto dto = new ServerSnapshotDto();
        dto.setId(snapshot.getId());
        dto.setServerId(snapshot.getServerId());
        dto.setPolledAt(snapshot.getPolledAt());
        dto.setHealthStatus(snapshot.getHealthStatus());
        dto.setAppVersion(snapshot.getAppVersion());
        dto.setMemoryUsedBytes(snapshot.getMemoryUsedBytes());
        dto.setCpuUsage(snapshot.getCpuUsage());
        dto.setSystemLoad(snapshot.getSystemLoad());
        dto.setUptimeSeconds(snapshot.getUptimeSeconds());
        dto.setJvmThreadsLive(snapshot.getJvmThreadsLive());
        dto.setGcOverhead(snapshot.getGcOverhead());
        dto.setDiskTotalBytes(snapshot.getDiskTotalBytes());
        dto.setDiskFreeBytes(snapshot.getDiskFreeBytes());
        dto.setHttpRequestCount(snapshot.getHttpRequestCount());
        dto.setHttpAvgMs(snapshot.getHttpAvgMs());
        dto.setHttp2xxCount(snapshot.getHttp2xxCount());
        dto.setHttp4xxCount(snapshot.getHttp4xxCount());
        dto.setHttp5xxCount(snapshot.getHttp5xxCount());
        dto.setPollSuccess(snapshot.isPollSuccess());
        return dto;
    }
}