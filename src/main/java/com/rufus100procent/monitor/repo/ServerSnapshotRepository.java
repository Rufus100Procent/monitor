package com.rufus100procent.monitor.repo;

import com.rufus100procent.monitor.modal.ServerSnapshot;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface ServerSnapshotRepository extends ReactiveCrudRepository<ServerSnapshot, UUID> {

    Flux<ServerSnapshot> findAllByServerIdOrderByPolledAtDesc(UUID serverId);

    Flux<ServerSnapshot> findTop10ByServerIdOrderByPolledAtDesc(UUID serverId);

    Flux<ServerSnapshot> findTop60ByServerIdOrderByPolledAtDesc(UUID serverId);

    Flux<ServerSnapshot> findAllByServerIdAndPolledAtBetweenOrderByPolledAtDesc(
            UUID serverId,
            Instant from,
            Instant to
    );

    Mono<ServerSnapshot> findTopByServerIdOrderByPolledAtDesc(UUID serverId);

    Flux<ServerSnapshot> findAllByServerIdAndPollSuccessTrueOrderByPolledAtDesc(UUID serverId);
}