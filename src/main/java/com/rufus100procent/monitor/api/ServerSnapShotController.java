package com.rufus100procent.monitor.api;

import com.rufus100procent.monitor.dto.ServerSnapshotDto;
import com.rufus100procent.monitor.service.ServerSnapshotService;
import com.rufus100procent.monitor.utils.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/snapshot")
@CrossOrigin(origins = "http://localhost:5173")
public class ServerSnapShotController {

    private final ServerSnapshotService snapshotService;

    public ServerSnapShotController(ServerSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @GetMapping("/{serverId}")
    public Flux<ServerSnapshotDto> getSnapshots(
            @PathVariable UUID serverId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false, defaultValue = "DESC") String sort,
            @RequestParam(required = false, defaultValue = "50") int limit) {

        return snapshotService.getSnapshots(serverId, from, to, sort, limit);
    }

    @GetMapping("/{serverId}/latest")
    public Mono<ResponseEntity<Object>> getLatest(@PathVariable UUID serverId) {
        return snapshotService.getLatest(serverId)
                .map(dto -> ResponseEntity.ok((Object) dto))
                .onErrorResume(ex -> ApiError.error(ex, HttpStatus.NOT_FOUND));
    }
}

/*
# latest snapshot
GET /api/v0/snapshot/{serverId}/latest

# last 50 newest first (default)
GET /api/v0/snapshot/{serverId}

# last 100 newest first
GET /api/v0/snapshot/{serverId}?limit=100

# oldest first
GET /api/v0/snapshot/{serverId}?sort=ASC

# specific date range
GET /api/v0/snapshot/{serverId}?from=2026-03-01T00:00:00Z&to=2026-03-01T23:59:59Z

# yesterday oldest to newest, 200 results
GET /api/v0/snapshot/{serverId}?from=2026-03-03T00:00:00Z&to=2026-03-03T23:59:59Z&sort=ASC&limit=200
 */
