package io.github.deeqma.monitor.api;

import io.github.deeqma.monitor.service.ServerSnapshotService;
import io.github.deeqma.monitor.utils.ApiError;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

import static io.github.deeqma.monitor.utils.JwtUtil.extractUserId;

@RestController
@RequestMapping("/api/v0/snapshot")
public class ServerSnapShotController {

    private final ServerSnapshotService snapshotService;

    public ServerSnapShotController(ServerSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @GetMapping("/{serverId}")
    public Mono<ResponseEntity<Object>> getSnapshots(
            @PathVariable UUID serverId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false, defaultValue = "50") int size,
            @RequestParam(required = false, defaultValue = "0") int page,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = extractUserId(jwt);
        return snapshotService.getSnapshots(serverId, userId, from, to, size, page)
                .map(response -> ResponseEntity.ok((Object) response))
                .onErrorResume(ex -> ApiError.error(ex, HttpStatus.NOT_FOUND));
    }

    @GetMapping("/{serverId}/latest")
    public Mono<ResponseEntity<Object>> getLatest(
            @PathVariable UUID serverId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = extractUserId(jwt);
        return snapshotService.getLatest(serverId, userId)
                .map(dto -> ResponseEntity.ok((Object) dto))
                .onErrorResume(ex -> ApiError.error(ex, HttpStatus.NOT_FOUND));
    }

    @GetMapping("/{serverId}/size")
    public Mono<ResponseEntity<Object>> getTableSize(
            @PathVariable UUID serverId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = extractUserId(jwt);
        return snapshotService.getTableSize(serverId, userId)
                .map(size -> ResponseEntity.ok((Object) size))
                .onErrorResume(ex -> ApiError.error(ex, HttpStatus.NOT_FOUND));
    }

    @GetMapping("/{serverId}/export")
    public Mono<ResponseEntity<Flux<String>>> exportCsv(
            @PathVariable UUID serverId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = extractUserId(jwt);
        Flux<String> csvStream = snapshotService.exportSnapshotsCsv(serverId, userId)
                .map(row -> row + "\n");

        return Mono.just(ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"snapshots-" + serverId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvStream));
    }


}

/*
# latest snapshot
GET /api/v0/snapshot/{serverId}/latest

# page 0, size 100 newest first
GET /api/v0/snapshot/{serverId}?size=100

# page 1, size 50 newest first
GET /api/v0/snapshot/{serverId}?page=1

# page 2, size 80 newest first
GET /api/v0/snapshot/{serverId}?page=2&size=80

# specific date range, page 0, size 50 newest first
GET /api/v0/snapshot/{serverId}?from=2026-03-01T00:00:00Z&to=2026-03-01T23:59:59Z

# specific date range, page 1, size 200
GET /api/v0/snapshot/{serverId}?from=2026-03-03T00:00:00Z&to=2026-03-03T23:59:59Z&page=1&size=200

GET /api/v0/snapshot/{serverId}?from=2026-03-01T00:00:00Z&to=2026-03-07T23:59:59Z&page=0&size=100
*/
