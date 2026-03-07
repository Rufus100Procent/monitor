package com.rufus100procent.monitor.api;

import com.rufus100procent.monitor.dto.RegisteredServerDto;
import com.rufus100procent.monitor.dto.ServerDto;
import com.rufus100procent.monitor.dto.UpdateRegisteredServerDto;
import com.rufus100procent.monitor.service.ServerRegisterService;
import com.rufus100procent.monitor.utils.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/server")
@CrossOrigin(origins = "http://localhost:5173")
public class RegisterServerController {

    private final ServerRegisterService registerService;

    public RegisterServerController(ServerRegisterService registerService) {
        this.registerService = registerService;
    }

    @PostMapping
    public Mono<ResponseEntity<Object>> register(@RequestBody ServerDto data) {
        return registerService.registerServer(data)
                .map(secret -> ResponseEntity.ok((Object) Map.of("secret", secret)))
                .onErrorResume(ex -> ApiError.error(ex, HttpStatus.BAD_REQUEST));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Object>> getById(@PathVariable UUID id) {
        return registerService.getServerById(id)
                .map(dto -> ResponseEntity.ok((Object) dto))
                .onErrorResume(ex -> ApiError.error(ex, HttpStatus.NOT_FOUND));
    }

    @GetMapping
    public Flux<RegisteredServerDto> getAll() {
        return registerService.getAllServers();
    }

    @PutMapping
    public Mono<ResponseEntity<Object>> update(@RequestBody UpdateRegisteredServerDto data) {
        return registerService.updateServer(data)
                .map(dto -> ResponseEntity.ok((Object) dto))
                .onErrorResume(ex -> ApiError.error(ex, HttpStatus.BAD_REQUEST));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Object>> delete(@PathVariable UUID id) {
        return registerService.deleteServer(id)
                .then(Mono.just(ResponseEntity.ok((Object) Map.of("message", "Server deleted successfully"))))
                .onErrorResume(ex -> ApiError.error(ex, HttpStatus.NOT_FOUND));
    }

//    test connection, check secret works

}
