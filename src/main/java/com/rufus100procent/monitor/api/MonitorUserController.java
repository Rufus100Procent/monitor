package com.rufus100procent.monitor.api;

import com.rufus100procent.monitor.service.MonitorUserService;
import com.rufus100procent.monitor.utils.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static com.rufus100procent.monitor.utils.JwtUtil.extractUserId;

@RestController
@RequestMapping("/api/v0/user")
public class MonitorUserController {

    private final MonitorUserService monitorUserService;

    public MonitorUserController(MonitorUserService monitorUserService) {
        this.monitorUserService = monitorUserService;
    }

    @PutMapping("/secret")
    public Mono<ResponseEntity<Object>> regenerateSecret(
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = extractUserId(jwt);
        return monitorUserService.regenerateSecret(userId)
                .map(secret -> ResponseEntity.ok((Object) secret))
                .onErrorResume(ex -> ApiError.error(ex, HttpStatus.NOT_FOUND));
    }

    @DeleteMapping
    public Mono<ResponseEntity<Object>> deleteUser(
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = extractUserId(jwt);
        return monitorUserService.deleteUser(userId)
                .then(Mono.just(ResponseEntity.ok((Object) "User deleted successfully")))
                .onErrorResume(ex -> ApiError.error(ex, HttpStatus.NOT_FOUND));
    }

    @GetMapping("/snapshots/size")
    public Mono<ResponseEntity<Object>> getTotalSnapshotSize(
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = extractUserId(jwt);
        return monitorUserService.getTotalSnapshotSize(userId)
                .map(size -> ResponseEntity.ok((Object) size))
                .onErrorResume(ex -> ApiError.error(ex, HttpStatus.INTERNAL_SERVER_ERROR));
    }

}