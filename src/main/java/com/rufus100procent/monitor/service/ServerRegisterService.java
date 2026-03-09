package com.rufus100procent.monitor.service;

import com.rufus100procent.monitor.dto.RegisteredServerDto;
import com.rufus100procent.monitor.dto.ServerDto;
import com.rufus100procent.monitor.dto.UpdateRegisteredServerDto;
import com.rufus100procent.monitor.modal.ServerRegister;
import com.rufus100procent.monitor.repo.ServerRegisterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
public class ServerRegisterService {

    private static final Logger log = LoggerFactory.getLogger(ServerRegisterService.class);

    private final ServerRegisterRepository serverRegisterRepository;
    private final ActuatorClient actuatorClient;

    public ServerRegisterService(ServerRegisterRepository serverRegisterRepository, ActuatorClient actuatorClient) {
        this.serverRegisterRepository = serverRegisterRepository;
        this.actuatorClient = actuatorClient;
    }

//    /actuator/metrics/jvm.memory.max
//    /actuator/metrics/system.cpu.count
    public Mono<String> registerServer(ServerDto data) {
        return serverRegisterRepository.existsByBaseUrlAndActuatorPath(
                        data.getBaseUrl(), data.getActuatorPath())
                .filter(exists -> !exists)
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "Server already registered with url: " + data.getBaseUrl() + data.getActuatorPath())))
                .flatMap(_ -> Mono.zip(
                        actuatorClient.fetchMemoryMax(data.getBaseUrl(), data.getActuatorPath()),
                        actuatorClient.fetchCpuCoreCount(data.getBaseUrl(), data.getActuatorPath())
                ))
                .flatMap(tuple -> {
                    long memoryMax   = tuple.getT1();
                    int cpuCoreCount = tuple.getT2();

                    if (memoryMax == 0L) {
                        return Mono.error(new IllegalStateException(
                                "Could not reach actuator at: " + data.getBaseUrl() + data.getActuatorPath()
                                        + " — is the actuator exposed and reachable?"));
                    }

                    ServerRegister register = new ServerRegister();
                    register.setId(UUID.randomUUID());
                    register.setUserId(UUID.randomUUID());
                    register.setRegisteredAt(Instant.now());
                    register.setStatus("UP");
                    register.setPause(false);
                    register.setSecret("monitor-v1-" + UUID.randomUUID());
                    register.setAppName(data.getAppName());
                    register.setAppVersion(data.getAppVersion());
                    register.setBaseUrl(data.getBaseUrl());
                    register.setActuatorPath(data.getActuatorPath());
                    register.setPollIntervalSeconds(data.getPollIntervalSeconds());
                    register.setMemoryMaxBytes(memoryMax);
                    register.setCpuCoreCount(cpuCoreCount);
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
                            .map(ServerRegister::getSecret);
                });
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

    public Mono<RegisteredServerDto> getServerById(UUID id) {
        return serverRegisterRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        " Server not found with id: " + id)))
                .map(this::toDto);
    }

    public Flux<RegisteredServerDto> getAllServers() {
        return serverRegisterRepository.findAll()
                .map(this::toDto);
    }

    public Mono<RegisteredServerDto> updateServer(UpdateRegisteredServerDto data) {
        return serverRegisterRepository.findById(data.getId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Server  not found with id: " + data.getId())))
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

    public Mono<Void> deleteServer(UUID id) {
        return serverRegisterRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Server not found with id: " + id)))
                .flatMap(server -> {
                    log.info("Deleting server id={} name={}", server.getId(), server.getAppName());
                    return serverRegisterRepository.delete(server);
                });
    }

    private RegisteredServerDto toDto(ServerRegister register) {
        RegisteredServerDto dto = new RegisteredServerDto();
        dto.setId(register.getId());
        dto.setAppName(register.getAppName());
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
