package com.diploma.atsbff.analysis;

import com.diploma.atsbff.common.ApiException;
import com.diploma.atsbff.config.AppProperties;
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

    public PythonAnalyticsService(RestClient.Builder builder, AppProperties properties) {
        this.restClient = builder.baseUrl(properties.getPython().getBaseUrl()).build();
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
                exception.getRawStatusCode(),
                detail
            ),
            exception
        );
    }
}
