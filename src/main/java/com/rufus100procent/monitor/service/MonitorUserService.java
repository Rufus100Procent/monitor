package com.rufus100procent.monitor.service;

import com.rufus100procent.monitor.modal.MonitorUser;
import com.rufus100procent.monitor.repo.ServerSnapshotRepository;
import com.rufus100procent.monitor.repo.UserRepository;
import com.rufus100procent.monitor.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class MonitorUserService {

    private static final Logger log = LoggerFactory.getLogger(MonitorUserService.class);

    private final UserRepository userRepository;
    private final ServerSnapshotRepository snapshotRepository;

    public MonitorUserService(UserRepository userRepository,
                              ServerSnapshotRepository snapshotRepository) {
        this.userRepository = userRepository;
        this.snapshotRepository = snapshotRepository;
    }

    public Mono<String> regenerateSecret(UUID userId) {
        return findUserById(userId)
                .flatMap(user -> {
                    user.setIsNew(false);
                    user.setSecret(generateSecret());
                    return userRepository.save(user)
                            .doOnSuccess(_ -> log.info("Secret regenerated for userId={}", userId))
                            .map(MonitorUser::getSecret);
                });
    }

    public Mono<Void> deleteUser(UUID userId) {
        return findUserById(userId)
                .flatMap(user -> {
                    log.info("Deleting user id={} username={}", user.getId(), user.getUsername());
                    return userRepository.delete(user);
                });
    }

    public Mono<String> getTotalSnapshotSize(UUID userId) {
        return findUserById(userId)
                .flatMap(_ -> snapshotRepository.getSnapshotSizeBytesByUserId(userId))
                .map(Utils::formatBytes);
    }

    public Mono<String> resolveUserSecret(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> resolveSecret(user.getSecret()))
                .defaultIfEmpty("NO_SECRET_CONFIGURED");
    }

    private String resolveSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            return "NO_SECRET_CONFIGURED";
        }
        return secret;
    }

    private Mono<MonitorUser> findUserById(UUID userId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "User not found with id: " + userId)));
    }

    private String generateSecret() {
        return "monitor-v1-" + UUID.randomUUID();
    }
}