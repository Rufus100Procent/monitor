package io.github.deeqma.monitor.service;

import io.github.deeqma.monitor.dto.RegisteredServerDto;
import io.github.deeqma.monitor.dto.ServerDto;
import io.github.deeqma.monitor.dto.UpdateRegisteredServerDto;
import io.github.deeqma.monitor.modal.ServerRegister;
import io.github.deeqma.monitor.repo.ServerRegisterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Instant;
import java.util.UUID;

@Service
public class ServerRegisterService {

    private static final Logger log = LoggerFactory.getLogger(ServerRegisterService.class);

    private final ServerRegisterRepository serverRegisterRepository;
    private final ActuatorClient actuatorClient;
    private final MonitorUserService monitorUserService;

    public ServerRegisterService(ServerRegisterRepository serverRegisterRepository, ActuatorClient actuatorClient, MonitorUserService monitorUserService) {
        this.serverRegisterRepository = serverRegisterRepository;
        this.actuatorClient = actuatorClient;
        this.monitorUserService = monitorUserService;
    }

//    /actuator/metrics/jvm.memory.max
//    /actuator/metrics/system.cpu.count
    public Mono<RegisteredServerDto> registerServer(ServerDto data, UUID userId) {
        return monitorUserService.resolveUserSecret(userId)
            .flatMap(secret -> serverRegisterRepository
                    .existsByBaseUrlAndActuatorPath(data.getBaseUrl(), data.getActuatorPath())
                    .filter(exists -> !exists)
                    .switchIfEmpty(Mono.error(new IllegalStateException(
                            "Server already registered with url: " + data.getBaseUrl() + data.getActuatorPath())))
                    .flatMap(_ -> validateActuatorUrl(data.getBaseUrl(), data.getActuatorPath(), secret))
                    .flatMap(tuple -> {
                        ServerRegister register = new ServerRegister();
                        register.setId(UUID.randomUUID());
                        register.setUserId(userId);
                        register.setRegisteredAt(Instant.now());
                        register.setStatus("UP");
                        register.setPause(false);
                        register.setAppName(data.getAppName());
                        register.setAppVersion(data.getAppVersion());
                        register.setBaseUrl(data.getBaseUrl());
                        register.setActuatorPath(data.getActuatorPath());
                        register.setPollIntervalSeconds(data.getPollIntervalSeconds());
                        register.setMemoryMaxBytes(tuple.getT1());
                        register.setCpuCoreCount(tuple.getT2());
                        register.setLastPolledAt(Instant.now());
                        register.setLastSeenUp(Instant.now());

                        return serverRegisterRepository.save(register)
                                .doOnSuccess(s -> {
                                    assert s != null;
                                    log.info(
                                            "Server registered id={} name={} url={}{} memoryMax={} cpuCores={}",
                                            s.getId(), s.getAppName(), s.getBaseUrl(), s.getActuatorPath(),
                                            s.getMemoryMaxBytes(), s.getCpuCoreCount());
                                })
                                .map(this::toDto);
                    })
            );
    }

    public Mono<Void> updateAfterPoll(ServerRegister server, String health) {
        server.setStatus(health);
        server.setLastPolledAt(Instant.now());
        if ("UP".equals(health)) {
            server.setLastSeenUp(Instant.now());
        }
        server.markAsExisting();
        return serverRegisterRepository.save(server)
                .doOnSuccess(_ -> {
                    if ("DOWN".equals(health)) {
                        log.warn("Server is DOWN name={} url={}{}",
                                server.getAppName(), server.getBaseUrl(), server.getActuatorPath());
                    }
                })
                .doOnError(e -> log.error("Failed to update server register after poll name={} reason={}",
                        server.getAppName(), e.getMessage()))
                .then();
    }

    public Mono<RegisteredServerDto> getServerById(UUID id, UUID userId) {
        return findServerByIdAndUserId(id, userId)
                .map(this::toDto);
    }

    public Flux<RegisteredServerDto> getAllServers(UUID userId) {
        return serverRegisterRepository.findAllByUserId(userId)
                .map(this::toDto);
    }

    public Mono<RegisteredServerDto> updateServer(UpdateRegisteredServerDto data, UUID userId) {
        return findServerByIdAndUserId(data.getId(), userId)
                .flatMap(register -> {
                    register.setAppName(data.getAppName());
                    register.setAppVersion(data.getAppVersion());
                    register.setPollIntervalSeconds(data.getPollIntervalSeconds());
                    register.setPause(data.isPause());
                    register.markAsExisting();
                    return serverRegisterRepository.save(register);
                })
                .doOnSuccess(s -> {
                    assert s != null;
                    log.info("Server updated id={} name={}", s.getId(), s.getAppName());
                })
                .map(this::toDto);
    }

    public Mono<Void> deleteServer(UUID id, UUID userId) {
        return findServerByIdAndUserId(id, userId)
                .flatMap(server -> {
                    log.info("Deleting server id={} name={}", server.getId(), server.getAppName());
                    return serverRegisterRepository.delete(server);
                });
    }

    private Mono<Tuple2<Long, Integer>> validateActuatorUrl(String baseUrl, String actuatorPath, String secret) {
        return Mono.zip(
                actuatorClient.fetchMemoryMax(baseUrl, actuatorPath, secret),
                actuatorClient.fetchCpuCoreCount(baseUrl, actuatorPath, secret)
        ).flatMap(tuple -> {
            if (tuple.getT1() == 0L) {
                return Mono.error(new IllegalStateException(
                        "Could not reach actuator at: " + baseUrl + actuatorPath
                                + " — is the actuator exposed and reachable?"));
            }
            return Mono.just(tuple);
        });
    }

    private Mono<ServerRegister> findServerByIdAndUserId(UUID serverId, UUID userId) {
        return serverRegisterRepository.findByIdAndUserId(serverId, userId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Server not found with id: " + serverId)));
    }

    private RegisteredServerDto toDto(ServerRegister register) {
        RegisteredServerDto dto = new RegisteredServerDto();
        dto.setId(register.getId());
        dto.setAppName(register.getAppName());
        dto.setAppVersion(register.getAppVersion());
        dto.setServerActuatorUrl(register.getBaseUrl() + register.getActuatorPath());
        dto.setPollIntervalSeconds(register.getPollIntervalSeconds());
        dto.setPause(register.isPause());
        dto.setRegisteredAt(register.getRegisteredAt());
        dto.setStatus(register.getStatus());
        dto.setCpuCoreCount(register.getCpuCoreCount());
        dto.setMemoryMaxBytes(register.getMemoryMaxBytes());
        dto.setLastPolledAt(register.getLastPolledAt());
        dto.setLastSeenUp(register.getLastSeenUp());
        return dto;
    }

}
