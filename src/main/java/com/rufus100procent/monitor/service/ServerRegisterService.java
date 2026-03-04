package com.rufus100procent.monitor.service;

import com.rufus100procent.monitor.dto.ServerDto;
import com.rufus100procent.monitor.modal.ServerRegister;
import com.rufus100procent.monitor.repo.ServerRegisterRepository;
import org.springframework.stereotype.Service;
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
            register.setActive(true);
            register.setSecret("monitor-v1-" + UUID.randomUUID());
            register.setName(data.getName());
            register.setBaseUrl(data.getBaseUrl());
            register.setActuatorPath(data.getActuatorPath());
            register.setPollIntervalSeconds(data.getPollIntervalSeconds());

            return serverRegisterRepository.save(register)
                    .map(ServerRegister::getSecret);
        });
    }


}
