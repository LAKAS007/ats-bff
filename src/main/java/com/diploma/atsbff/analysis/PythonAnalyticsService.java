package com.diploma.atsbff.analysis;

import com.diploma.atsbff.common.ApiException;
import com.diploma.atsbff.config.AppProperties;
import com.diploma.atsbff.demo.DemoDataService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

@Service
public class PythonAnalyticsService {

    private final RestClient restClient;
    private final AppProperties properties;
    private final DemoDataService demoDataService;

    public PythonAnalyticsService(
        RestClient.Builder builder,
        AppProperties properties,
        DemoDataService demoDataService
    ) {
        this.restClient = builder.baseUrl(properties.getPython().getBaseUrl()).build();
        this.properties = properties;
        this.demoDataService = demoDataService;
    }

    public JsonNode runAnalysis(AnalysisRequest request) {
        try {
            return restClient.post()
                .uri("/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(JsonNode.class);
        } catch (RestClientResponseException exception) {
            throw mapUpstreamException("analysis", exception);
        } catch (ResourceAccessException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Python analysis service is unavailable", exception);
        } catch (RestClientException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Python analysis request failed unexpectedly", exception);
        }
    }

    public String runMarkdownAnalysis(AnalysisRequest request) {
        if (properties.isDemoMode() || request.isDemoMode()) {
            pauseBeforeDemoMarkdown();
            return demoDataService.markdownReport(request);
        }

        try {
            return restClient.post()
                .uri("/analyze/markdown")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);
        } catch (RestClientResponseException exception) {
            throw mapUpstreamException("markdown analysis", exception);
        } catch (ResourceAccessException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Python markdown service is unavailable", exception);
        } catch (RestClientException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Python markdown request failed unexpectedly", exception);
        }
    }

    private ApiException mapUpstreamException(String operation, RestClientResponseException exception) {
        String responseBody = exception.getResponseBodyAsString();
        String detail = responseBody == null || responseBody.isBlank()
            ? exception.getStatusText()
            : responseBody;

        return new ApiException(
            HttpStatus.BAD_GATEWAY,
                "Python %s failed with status %d: %s".formatted(
                operation,
                exception.getStatusCode().value(),
                detail
            ),
            exception
        );
    }

    private void pauseBeforeDemoMarkdown() {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Demo markdown generation was interrupted", exception);
        }
    }
}
