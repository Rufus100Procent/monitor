package com.rufus100procent.monitor.repo;

import com.rufus100procent.monitor.modal.ServerSnapshot;
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
            ORDER BY
                CASE WHEN :sort = 'ASC' THEN polled_at END ASC,
                CASE WHEN :sort = 'DESC' THEN polled_at END DESC
            LIMIT :limit
            """)
    Flux<ServerSnapshot> findSnapshots(UUID serverId,
                                       Instant from,
                                       Instant to,
                                       String sort,
                                       int limit);

    Mono<ServerSnapshot> findTopByServerIdOrderByPolledAtDesc(UUID serverId);
}