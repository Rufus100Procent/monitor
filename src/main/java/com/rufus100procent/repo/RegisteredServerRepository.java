package com.rufus100procent.repo;

import com.rufus100procent.monitor.modal.RegisteredServer;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface RegisteredServerRepository extends ReactiveCrudRepository<RegisteredServer, UUID> {

    Flux<RegisteredServer> findAllByActiveTrue();

    Mono<RegisteredServer> findByBaseUrlAndActuatorPath(String baseUrl, String actuatorPath);

    Mono<Boolean> existsByBaseUrlAndActuatorPath(String baseUrl, String actuatorPath);
}