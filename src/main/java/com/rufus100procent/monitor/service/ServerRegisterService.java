package com.rufus100procent.monitor.service;

import com.rufus100procent.monitor.dto.RegisteredServerDto;
import com.rufus100procent.monitor.dto.ServerDto;
import com.rufus100procent.monitor.modal.ServerRegister;
import com.rufus100procent.monitor.repo.ServerRegisterRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
public class ServerRegisterService {

    private final ServerRegisterRepository serverRegisterRepository;

    public ServerRegisterService(ServerRegisterRepository serverRegisterRepository) {
        this.serverRegisterRepository = serverRegisterRepository;
    }

    public Mono<String> registerServer(ServerDto data) {

        return serverRegisterRepository.existsByBaseUrlAndActuatorPath(
                        data.getBaseUrl(), data.getActuatorPath())
                .filter(exists -> !exists)
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "Server already registered with url: " + data.getBaseUrl() + data.getActuatorPath()
                )))
                .flatMap(_ -> {

            ServerRegister register = new ServerRegister();
            register.setId(UUID.randomUUID());
            register.setRegisteredAt(Instant.now());
            register.setStatus("UNKNOWN");
            register.setPause(true);
            register.setSecret("monitor-v1-" + UUID.randomUUID());
            register.setName(data.getName());
            register.setBaseUrl(data.getBaseUrl());
            register.setActuatorPath(data.getActuatorPath());
            register.setPollIntervalSeconds(data.getPollIntervalSeconds());

            return serverRegisterRepository.save(register)
                    .map(ServerRegister::getSecret);
        });
    }

    public Mono<RegisteredServerDto> getServerById(UUID id) {
        return serverRegisterRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Server not found with id: " + id
                )))
                .map(this::toDto);
    }

    public Flux<RegisteredServerDto> getAllServers() {
        return serverRegisterRepository.findAll()
                .map(this::toDto);
    }

    private RegisteredServerDto toDto(ServerRegister register) {
        RegisteredServerDto dto = new RegisteredServerDto();
        dto.setId(register.getId());
        dto.setAppName(register.getName());
        dto.setServerActuatorUrl(register.getBaseUrl() + register.getActuatorPath());
        dto.setPollIntervalSeconds(register.getPollIntervalSeconds());
        dto.setPause(register.isPause());
        dto.setRegisteredAt(register.getRegisteredAt());
        dto.setStatus(register.getStatus());
        dto.setLastPolledAt(register.getLastPolledAt());
        dto.setLastSeenUp(register.getLastSeenUp());
        return dto;
    }
}
