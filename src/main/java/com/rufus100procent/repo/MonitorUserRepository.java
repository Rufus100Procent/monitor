package com.rufus100procent.repo;

import com.rufus100procent.monitor.modal.MonitorUser;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface MonitorUserRepository extends ReactiveCrudRepository<MonitorUser, UUID> {

    Mono<MonitorUser> findByUsername(String username);

    Mono<Boolean> existsByUsername(String username);
}