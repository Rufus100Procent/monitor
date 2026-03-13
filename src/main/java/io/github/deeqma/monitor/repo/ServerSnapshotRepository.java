package io.github.deeqma.monitor.repo;

import io.github.deeqma.monitor.modal.ServerSnapshot;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface ServerSnapshotRepository extends ReactiveCrudRepository<ServerSnapshot, UUID> {

    @Query("""
            SELECT * FROM server_snapshots
            WHERE server_id = :serverId
            AND polled_at BETWEEN :from AND :to
            ORDER BY polled_at DESC
            LIMIT :size OFFSET :offset
            """)
    Flux<ServerSnapshot> findSnapshots(UUID serverId,
                                       Instant from,
                                       Instant to,
                                       int size,
                                       int offset);

    @Query("""
            SELECT COUNT(*) FROM server_snapshots
            WHERE server_id = :serverId
            AND polled_at BETWEEN :from AND :to
            """)
    Mono<Long> countSnapshots(UUID serverId, Instant from, Instant to);


    Mono<ServerSnapshot> findTopByServerIdOrderByPolledAtDesc(UUID serverId);

    @Query("SELECT COALESCE(SUM(pg_column_size(s.*)), 0) FROM server_snapshots s WHERE s.server_id = :serverId")
    Mono<Long> getSnapshotSizeBytesByServerId(UUID serverId);

    @Query("SELECT COALESCE(SUM(pg_column_size(s.*)), 0) FROM server_snapshots s WHERE s.server_id IN (SELECT id FROM server_register WHERE user_id = :userId)")
    Mono<Long> getSnapshotSizeBytesByUserId(UUID userId);

    @Query("SELECT * FROM server_snapshots WHERE server_id = :serverId ORDER BY polled_at DESC")
    Flux<ServerSnapshot> findAllByServerIdOrderByPolledAtDesc(UUID serverId);
}