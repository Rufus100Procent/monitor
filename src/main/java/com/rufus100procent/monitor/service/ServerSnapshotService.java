package com.rufus100procent.monitor.service;

import com.rufus100procent.monitor.dto.ServerSnapshotDto;
import com.rufus100procent.monitor.modal.ServerRegister;
import com.rufus100procent.monitor.modal.ServerSnapshot;
import com.rufus100procent.monitor.repo.ServerRegisterRepository;
import com.rufus100procent.monitor.repo.ServerSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ServerSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(ServerSnapshotService.class);
    private static final int DEFAULT_SIZE = 50;

    private final ServerSnapshotRepository snapshotRepository;
    private final ServerRegisterRepository serverRegisterRepository;

    public ServerSnapshotService(ServerSnapshotRepository snapshotRepository,
                                 ServerRegisterRepository serverRegisterRepository) {
        this.snapshotRepository = snapshotRepository;
        this.serverRegisterRepository = serverRegisterRepository;
    }
    public record PagedSnapshotResponse(
            List<ServerSnapshotDto> data,
            long totalRows,
            int page,
            int size,
            long totalPages,
            boolean hasNext,
            boolean hasPrevious
    ) {}

    public Mono<PagedSnapshotResponse> getSnapshots(UUID serverId,
                                                    UUID userId,
                                                    Instant from,
                                                    Instant to,
                                                    int size,
                                                    int page) {
        int resolvedSize     = size > 0 ? size : DEFAULT_SIZE;
        int resolvedPage     = Math.max(page, 0);
        int offset           = resolvedPage * resolvedSize;
        Instant resolvedFrom = from != null ? from : Instant.EPOCH;
        Instant resolvedTo   = to != null ? to : Instant.now();

        return serverRegisterRepository.findByIdAndUserId(serverId, userId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Server not found with id: " + serverId)))
                .flatMap(_ -> Mono.zip(
                        snapshotRepository.findSnapshots(serverId, resolvedFrom, resolvedTo, resolvedSize, offset)
                                .map(this::toDto)
                                .collectList(),
                        snapshotRepository.countSnapshots(serverId, resolvedFrom, resolvedTo)
                ))
                .map(tuple -> {
                    List<ServerSnapshotDto> data = tuple.getT1();
                    long totalRows  = tuple.getT2();
                    long totalPages = (long) Math.ceil((double) totalRows / resolvedSize);
                    return new PagedSnapshotResponse(
                            data,
                            totalRows,
                            resolvedPage,
                            resolvedSize,
                            totalPages,
                            resolvedPage < totalPages - 1,
                            resolvedPage > 0
                    );
                });
    }

    public Mono<ServerSnapshotDto> getLatest(UUID serverId, UUID userId) {
        return serverRegisterRepository.findByIdAndUserId(serverId, userId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Server not found with id: " + serverId)))
                .flatMap(_ -> snapshotRepository.findTopByServerIdOrderByPolledAtDesc(serverId))
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "No snapshots found for server: " + serverId)))
                .map(this::toDto);
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
        snapshot.setMemoryUsedBytes(0L);
        snapshot.setCpuUsage(0.0);
        snapshot.setSystemLoad(0.0);
        snapshot.setUptimeSeconds(0.0);
        snapshot.setJvmThreadsLive(0L);
        snapshot.setGcOverhead(0.0);
        snapshot.setHttpRequestCount(0L);
        snapshot.setHttpAvgMs(0.0);
        snapshot.setHttp2xxCount(0L);
        snapshot.setHttp3xxCount(0L);
        snapshot.setHttp4xxCount(0L);
        snapshot.setHttp5xxCount(0L);
        snapshot.setDiskTotalBytes(0L);
        snapshot.setDiskFreeBytes(0L);
        snapshot.setPollSuccess(false);
        return snapshotRepository.save(snapshot)
                .doOnError(e -> log.error("Failed to save failed-snapshot for server={} reason={}",
                        server.getAppName(), e.getMessage()));
    }

    private ServerSnapshotDto toDto(ServerSnapshot snapshot) {
        ServerSnapshotDto dto = new ServerSnapshotDto();
        dto.setId(snapshot.getId());
        dto.setServerId(snapshot.getServerId());
        dto.setPolledAt(snapshot.getPolledAt());
        dto.setHealthStatus(snapshot.getHealthStatus());
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
        dto.setHttp3xxCount(snapshot.getHttp3xxCount());
        dto.setHttp4xxCount(snapshot.getHttp4xxCount());
        dto.setHttp5xxCount(snapshot.getHttp5xxCount());
        dto.setPollSuccess(snapshot.isPollSuccess());
        return dto;
    }
    public Mono<ServerSnapshot> findLatestRaw(UUID serverId) {
        return snapshotRepository.findTopByServerIdOrderByPolledAtDesc(serverId);
    }
}