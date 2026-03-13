package io.github.deeqma.monitor.utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final  class ApiError {

    private ApiError() {
    }

    public static Mono<ResponseEntity<Object>> error(Throwable ex, HttpStatus status) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", ex.getMessage());
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        return Mono.just(ResponseEntity.status(status).body(body));
    }

}
