package com.rufus100procent.monitor.api;

import com.rufus100procent.monitor.service.ServerUrlValidationService;
import com.rufus100procent.utils.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


@RestController
public class ServerController {

    private final ServerUrlValidationService validateUrl;

    public ServerController(ServerUrlValidationService validateUrl) {
        this.validateUrl = validateUrl;
    }

    @GetMapping("/validate")
    public Mono<ResponseEntity<Object>> validate(@RequestParam String baseUrl) {
        return validateUrl.validateUrl(baseUrl)
                .map(dto -> ResponseEntity.ok((Object) dto))
                .onErrorResume(ex -> ApiError.error(ex, HttpStatus.SERVICE_UNAVAILABLE));
    }

}