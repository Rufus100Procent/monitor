package io.github.deeqma.monitor.repo;

import io.github.deeqma.monitor.modal.ServerRegister;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface ServerRegisterRepository extends ReactiveCrudRepository<ServerRegister, UUID> {
    Mono<Boolean> existsByBaseUrlAndActuatorPath(String baseUrl, String actuatorPath);

    Flux<ServerRegister> findAllByPauseFalse();

    Mono<ServerRegister> findByIdAndUserId(UUID id, UUID userId);
    Flux<ServerRegister> findAllByUserId(UUID userId);
}