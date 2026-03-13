package io.github.deeqma.monitor.api;

import io.github.deeqma.monitor.dto.RegisteredServerDto;
import io.github.deeqma.monitor.dto.ServerDto;
import io.github.deeqma.monitor.dto.UpdateRegisteredServerDto;
import io.github.deeqma.monitor.service.ServerRegisterService;
import io.github.deeqma.monitor.utils.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

import static io.github.deeqma.monitor.utils.JwtUtil.extractUserId;

@RestController
@RequestMapping("/api/v0/server")
public class RegisterServerController {

    private final ServerRegisterService registerService;

    public RegisterServerController(ServerRegisterService registerService) {
        this.registerService = registerService;
    }

    @PostMapping
    public Mono<ResponseEntity<Object>> register(@RequestBody ServerDto data,
                                                 @AuthenticationPrincipal Jwt jwt) {
        UUID userId = extractUserId(jwt);
        return registerService.registerServer(data, userId)
                .map(secret -> ResponseEntity.ok((Object) Map.of("secret", secret)))
                .onErrorResume(ex -> ApiError.error(ex, HttpStatus.BAD_REQUEST));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Object>> getById(@PathVariable UUID id,
                                                @AuthenticationPrincipal Jwt jwt) {
        UUID userId = extractUserId(jwt);
        return registerService.getServerById(id, userId)
                .map(dto -> ResponseEntity.ok((Object) dto))
                .onErrorResume(ex -> ApiError.error(ex, HttpStatus.NOT_FOUND));
    }

    @GetMapping
    public Flux<RegisteredServerDto> getAll(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = extractUserId(jwt);
        return registerService.getAllServers(userId);
    }

    @PutMapping
    public Mono<ResponseEntity<Object>> update(@RequestBody UpdateRegisteredServerDto data,
                                               @AuthenticationPrincipal Jwt jwt) {
        UUID userId = extractUserId(jwt);
        return registerService.updateServer(data, userId)
                .map(dto -> ResponseEntity.ok((Object) dto))
                .onErrorResume(ex -> ApiError.error(ex, HttpStatus.BAD_REQUEST));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Object>> delete(@PathVariable UUID id,
                                               @AuthenticationPrincipal Jwt jwt) {
        UUID userId = extractUserId(jwt);
        return registerService.deleteServer(id, userId)
                .then(Mono.just(ResponseEntity.ok((Object) Map.of("message", "Server deleted successfully"))))
                .onErrorResume(ex -> ApiError.error(ex, HttpStatus.NOT_FOUND));
    }

}
