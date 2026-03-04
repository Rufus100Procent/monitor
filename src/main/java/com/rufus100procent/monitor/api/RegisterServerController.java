package com.rufus100procent.monitor.api;

import com.rufus100procent.monitor.dto.ServerDto;
import com.rufus100procent.monitor.service.ServerRegisterService;
import com.rufus100procent.utils.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/v0/server")
public class RegisterServerController {

    private final ServerRegisterService registerService;

    public RegisterServerController(ServerRegisterService registerService) {
        this.registerService = registerService;
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<Object>> register(@RequestBody ServerDto data) {
        return registerService.registerServer(data)
                .map(secret -> ResponseEntity.ok((Object) Map.of("secret", secret)))
                .onErrorResume(ex -> ApiError.error(ex, HttpStatus.BAD_REQUEST));
    }

//    list all registered servers

//    get server details by id

//    update server name, url, interval

//    remove server from monitoring (delete)

//    stop polling this server (pause or unpause, boolean pause true or false)

//    test connection, check secret works
}
