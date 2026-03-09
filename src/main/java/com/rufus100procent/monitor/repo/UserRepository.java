package com.rufus100procent.monitor.repo;

import com.rufus100procent.monitor.modal.MonitorUser;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserRepository extends ReactiveCrudRepository<MonitorUser, UUID> {
    Mono<MonitorUser> findByUsername(String username);
    Mono<Boolean> existsByUsername(String username);
}