package com.rufus100procent.monitor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

@Component
public class ActuatorClient {

    private static final Logger log = LoggerFactory.getLogger(ActuatorClient.class);
    private final WebClient webClient;

    public ActuatorClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<String> fetchHealth(String baseUrl, String actuatorPath) {
        return get(baseUrl + actuatorPath + "/health")
                .map(json -> json
                        .path("status")
                        .asString("UNKNOWN"))
                .onErrorReturn("DOWN");
    }

    public Mono<long[]> fetchHealthAndDisk(String baseUrl, String actuatorPath) {
        return get(baseUrl + actuatorPath + "/health")
                .map(json -> new long[]{json.path("components")
                                .path("diskSpace")
                                .path("details")
                                .path("total")
                                .asLong(0L),
                        json.path("components")
                                .path("diskSpace")
                                .path("details")
                                .path("free")
                                .asLong(0L)
                })
                .onErrorReturn(new long[]{0L, 0L});
    }

    public Mono<String> fetchAppName(String baseUrl, String actuatorPath) {
        return get(baseUrl + actuatorPath + "/info")
                .map(json -> json
                        .path("app")
                        .path("name")
                        .asString("unknown"))
                .onErrorReturn("unknown");
    }

    public Mono<String> fetchAppVersion(String baseUrl, String actuatorPath) {
        return get(baseUrl + actuatorPath + "/info")
                .map(json -> json
                        .path("app")
                        .path("version")
                        .asString("unknown"))
                .onErrorReturn("unknown");
    }

    public Mono<Double> fetchMetricValue(String baseUrl, String actuatorPath, String metricName) {
        return get(baseUrl + actuatorPath + "/metrics/" + metricName)
                .map(json -> extractStatistic(json, "VALUE"))
                .onErrorReturn(0.0);
    }

    public Mono<Double> fetchMetricStatistic(String baseUrl, String actuatorPath,
                                             String metricName, String statistic) {
        return get(baseUrl + actuatorPath + "/metrics/" + metricName)
                .map(json -> extractStatistic(json, statistic))
                .onErrorReturn(0.0);
    }

    public Mono<Double> fetchMetricValueByTag(String baseUrl, String actuatorPath,
                                              String metricName, String tag) {
        return get(baseUrl + actuatorPath + "/metrics/" + metricName + "?tag=outcome:" + tag)
                .map(json -> extractStatistic(json, "COUNT"))
                .onErrorReturn(0.0);
    }

    private Mono<JsonNode> get(String url) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    private double extractStatistic(JsonNode json, String statistic) {
        for (JsonNode measurement : json.path("measurements")) {
            if (statistic.equalsIgnoreCase(measurement.path("statistic").asString())) {
                return measurement.path("value").asDouble(0.0);
            }
        }
        log.warn("Statistic '{}' not found in measurements, returning 0.0", statistic);
        return 0.0;
    }
}