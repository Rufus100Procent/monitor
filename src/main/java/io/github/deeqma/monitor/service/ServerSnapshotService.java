package io.github.deeqma.monitor.service;

import io.github.deeqma.monitor.dto.ServerSnapshotDto;
import io.github.deeqma.monitor.modal.ServerRegister;
import io.github.deeqma.monitor.modal.ServerSnapshot;
import io.github.deeqma.monitor.repo.ServerRegisterRepository;
import io.github.deeqma.monitor.repo.ServerSnapshotRepository;
import io.github.deeqma.monitor.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
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
                        " Server not found with id: " + serverId)))
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
                        "Server  not found with id: " + serverId)))
                .flatMap(_ -> snapshotRepository.findTopByServerIdOrderByPolledAtDesc(serverId))
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "No snapshots found for server: " + serverId)))
                .map(this::toDto);
    }

    public Flux<String> exportSnapshotsCsv(UUID serverId, UUID userId) {
        return serverRegisterRepository.findByIdAndUserId(serverId, userId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Server not found with id: " + serverId)))
                .flatMapMany(_ -> snapshotRepository.findAllByServerIdOrderByPolledAtDesc(serverId))
                .map(this::toCsvRow)
                .startWith(CSV_HEADER);
    }

    private static final String CSV_HEADER =
            "id,server_id,polled_at,health_status,memory_used_bytes,cpu_usage," +
                    "system_load,uptime_seconds,jvm_threads_live,gc_overhead," +
                    "disk_total_bytes,disk_free_bytes,http_request_count,http_avg_ms," +
                    "http_2xx_count,http_3xx_count,http_4xx_count,http_5xx_count,poll_success";

    private String toCsvRow(ServerSnapshot s) {
        return String.join(",",
                str(s.getId()),
                str(s.getServerId()),
                str(s.getPolledAt()),
                str(s.getHealthStatus()),
                str(s.getMemoryUsedBytes()),
                str(s.getCpuUsage()),
                str(s.getSystemLoad()),
                str(s.getUptimeSeconds()),
                str(s.getJvmThreadsLive()),
                str(s.getGcOverhead()),
                str(s.getDiskTotalBytes()),
                str(s.getDiskFreeBytes()),
                str(s.getHttpRequestCount()),
                str(s.getHttpAvgMs()),
                str(s.getHttp2xxCount()),
                str(s.getHttp3xxCount()),
                str(s.getHttp4xxCount()),
                str(s.getHttp5xxCount()),
                str(s.isPollSuccess())
        );
    }

    public Mono<String> getTableSize(UUID serverId, UUID userId) {
        return serverRegisterRepository.findByIdAndUserId(serverId, userId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Server not found with id: " + serverId)))
                .flatMap(_ -> snapshotRepository.getSnapshotSizeBytesByServerId(serverId))
                .map(Utils::formatBytes);
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

    public Mono<ServerSnapshot> findLatestRaw(UUID serverId) {
        return snapshotRepository.findTopByServerIdOrderByPolledAtDesc(serverId);
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

    private String str(Object val) {
        return val == null ? "" : val.toString();
    }
}