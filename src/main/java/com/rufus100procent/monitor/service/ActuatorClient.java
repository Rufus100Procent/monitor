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

    public Mono<Long> fetchMemoryMax(String baseUrl, String actuatorPath) {
        return get(baseUrl + actuatorPath + "/metrics/jvm.memory.max")
                .map(json -> extractStatistic(json, "VALUE"))
                .map(Double::longValue)
                .onErrorReturn(0L);
    }

    public Mono<Integer> fetchCpuCoreCount(String baseUrl, String actuatorPath) {
        return get(baseUrl + actuatorPath + "/metrics/system.cpu.count")
                .map(json -> extractStatistic(json, "VALUE"))
                .map(Double::intValue)
                .onErrorMap(ex -> new IllegalStateException(
                        "Could not reach actuator at: " + baseUrl + actuatorPath + " reason: " + ex.getMessage()));
    }

    // health status + disk in one call
    public Mono<HealthResult> fetchHealth(String baseUrl, String actuatorPath) {
        return get(baseUrl + actuatorPath + "/health")
                .map(json -> {
                    String status = json.path("status").asString("UNKNOWN");
                    long diskTotal = json.path("components").path("diskSpace")
                            .path("details").path("total").asLong(0L);
                    long diskFree = json.path("components").path("diskSpace")
                            .path("details").path("free").asLong(0L);
                    return new HealthResult(status, diskTotal, diskFree);
                })
                .onErrorReturn(new HealthResult("DOWN", 0L, 0L));
    }

    public Mono<String> fetchAppVersion(String baseUrl, String actuatorPath) {
        return get(baseUrl + actuatorPath + "/info")
                .map(json -> json.path("build").path("version").asString("unknown"))
                .onErrorReturn("unknown");
    }

    public Mono<Double> fetchMetric(String baseUrl, String actuatorPath, String metricName) {
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

    public Mono<Double> fetchMetricByOutcome(String baseUrl, String actuatorPath,
                                             String metricName, String outcome) {
        return get(baseUrl + actuatorPath + "/metrics/" + metricName + "?tag=outcome:" + outcome)
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

    public record HealthResult(String status, long diskTotal, long diskFree) {}
}