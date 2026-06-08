package com.diploma.atsbff.common;

import com.diploma.atsbff.config.AppProperties;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final AppProperties properties;
    private final RestClient pythonClient;
    private final RestClient bybitClient;
    private final RestClient ollamaClient;

    public HealthController(AppProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.pythonClient = builder.clone().baseUrl(properties.getPython().getBaseUrl()).build();
        this.bybitClient = builder.clone().baseUrl(properties.getBybit().getBaseUrl()).build();
        this.ollamaClient = builder.clone().baseUrl(properties.getOllama().getBaseUrl()).build();
    }

    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
            "status", "healthy",
            "service", "ats-bff",
            "demoMode", properties.isDemoMode(),
            "timestamp", Instant.now().toString()
        );
    }

    @GetMapping("/dependencies")
    public Map<String, Object> dependencies() {
        Map<String, String> dependencies = new LinkedHashMap<>();
        dependencies.put("bff", "ok");

        dependencies.put("pythonAtsService", ping(pythonClient, "/health"));
        dependencies.put("bybit", ping(bybitClient, "/v5/market/time"));
        dependencies.put("llm", ping(ollamaClient, "/api/tags"));

        boolean degraded = dependencies.values().stream().anyMatch("unavailable"::equals);
        return Map.of(
            "status", degraded ? "degraded" : "healthy",
            "service", "ats-bff",
            "demoMode", properties.isDemoMode(),
            "dependencies", dependencies,
            "timestamp", Instant.now().toString()
        );
    }

    private String ping(RestClient client, String path) {
        try {
            client.get().uri(path).retrieve().toBodilessEntity();
            return "ok";
        } catch (RestClientException exception) {
            return "unavailable";
        }
    }
}
