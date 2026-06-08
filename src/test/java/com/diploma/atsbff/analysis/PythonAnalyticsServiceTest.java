package com.diploma.atsbff.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.diploma.atsbff.config.AppProperties;
import com.diploma.atsbff.demo.DemoDataService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class PythonAnalyticsServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void jsonAnalysisUsesPythonEvenWhenDemoModeIsEnabled() throws Exception {
        AtomicInteger analysisHits = new AtomicInteger();
        startServer(exchange -> {
            analysisHits.incrementAndGet();
            send(exchange, 200, "{\"source\":\"python\",\"approved\":true}");
        });

        PythonAnalyticsService service = service(true);

        JsonNode response = service.runAnalysis(new AnalysisRequest("BTC", new BigDecimal("100000"), "demo"));

        assertThat(analysisHits).hasValue(1);
        assertThat(response.path("source").asText()).isEqualTo("python");
        assertThat(response.path("approved").asBoolean()).isTrue();
    }

    @Test
    void demoMarkdownReturnsPreparedMarkdownWithVisibleDelay() throws Exception {
        AtomicInteger markdownHits = new AtomicInteger();
        startServer(exchange -> {
            markdownHits.incrementAndGet();
            send(exchange, 200, "# Live markdown");
        });

        PythonAnalyticsService service = service(false);
        Instant startedAt = Instant.now();

        String markdown = service.runMarkdownAnalysis(new AnalysisRequest("ETH", new BigDecimal("50000"), "demo"));

        assertThat(markdownHits).hasValue(0);
        assertThat(Duration.between(startedAt, Instant.now()).toMillis()).isGreaterThanOrEqualTo(1000);
        assertThat(markdown).contains("# ATS Demo Report: ETH");
        assertThat(markdown).contains("Position size: 16% of NAV");
    }

    private PythonAnalyticsService service(boolean demoMode) {
        AppProperties properties = new AppProperties();
        properties.setDemoMode(demoMode);
        properties.getPython().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());

        return new PythonAnalyticsService(
            RestClient.builder(),
            properties,
            new DemoDataService(objectMapper)
        );
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/analyze", handler::handle);
        server.createContext("/analyze/markdown", handler::handle);
        server.start();
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, payload.length);
        exchange.getResponseBody().write(payload);
        exchange.close();
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
