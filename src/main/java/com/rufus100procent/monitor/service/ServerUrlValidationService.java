package com.rufus100procent.monitor.service;

import com.rufus100procent.monitor.dto.ServerDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

@Service
public class ServerUrlValidationService {

    private static final Logger log = LoggerFactory.getLogger(ServerUrlValidationService.class);
    private static final String DEFAULT_ACTUATOR_PATH = "/actuator";
    private static final String HEALTH_PATH = "/health";
    private static final int DEFAULT_POLL_INTERVAL = 7;

    private final WebClient webClient;

    public ServerUrlValidationService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<ServerDto> validateUrl(String baseUrl) {
        log.debug("Validating server with baseUrl: {}", baseUrl);

        String cleanBase = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        String resolvedBase;
        String actuatorPath;

        if (cleanBase.contains(DEFAULT_ACTUATOR_PATH)) {
            int index = cleanBase.indexOf(DEFAULT_ACTUATOR_PATH);
            resolvedBase = cleanBase.substring(0, index);
            actuatorPath = cleanBase.substring(index);
            log.debug("Actuator path found in url, resolvedBase={} actuatorPath={}", resolvedBase, actuatorPath);
        } else {
            resolvedBase = cleanBase;
            actuatorPath = DEFAULT_ACTUATOR_PATH;
            log.debug("No actuator path in url, using default. resolvedBase={} actuatorPath={}", resolvedBase, actuatorPath);
        }

        String healthUrl = resolvedBase + actuatorPath + HEALTH_PATH;
        String finalBase = resolvedBase;
        String finalActuatorPath = actuatorPath;

        log.info("Calling health endpoint: {}", healthUrl);

        return callHealth(healthUrl, finalBase)
                .map(status -> {
                    log.info("Health check passed, status={} url={}", status, healthUrl);
                    ServerDto dto = new ServerDto();
                    dto.setAppName(finalBase);
                    dto.setBaseUrl(finalBase);
                    dto.setActuatorPath(finalActuatorPath);
                    dto.setPollIntervalSeconds(DEFAULT_POLL_INTERVAL);
                    return dto;
                });
    }

    private Mono<String> callHealth(String healthUrl, String resolvedBase) {
        return webClient.get()
                .uri(healthUrl)
                .retrieve()
                .onStatus(
                        status -> status.value() == 404,

                        _ -> {
                            String msg = "Could not find actuator path at: " + healthUrl
                                    + " — try passing the full actuator URL instead of: " + resolvedBase;
                            log.warn(msg);
                            return Mono.error(new IllegalArgumentException(msg));
                        }
                )
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> {
                            String msg = "Server returned error status: " + response.statusCode().value()
                                    + " at: " + healthUrl;
                            log.warn(msg);
                            return Mono.error(new IllegalStateException(msg));
                        }
                )
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    log.debug("Raw health response: {}", response);
                    String status = response.path("status").asString();
                    log.debug("Parsed health status: {}", status);
                    if (!"UP".equalsIgnoreCase(status)) {
                        String msg = "Server is not UP, current status is: " + status + " at: " + healthUrl;
                        log.warn(msg);
                        throw new IllegalStateException(msg);
                    }
                    return status;
                })
                .onErrorMap(
                        ex -> !(ex instanceof IllegalArgumentException) && !(ex instanceof IllegalStateException),
                        ex -> {
                            String msg = "Server not reachable at: " + healthUrl
                                    + " — ensure the server is running and the URL is correct";
                            log.error("{} reason: {}", msg, ex.getMessage());
                            return new IllegalStateException(msg);
                        }
                );
    }
}
