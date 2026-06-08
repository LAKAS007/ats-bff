package com.diploma.atsbff.assistant;

import com.diploma.atsbff.common.ApiException;
import com.diploma.atsbff.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class OllamaChatService {

    private final RestClient restClient;
    private final AppProperties properties;

    public OllamaChatService(
        RestClient.Builder builder,
        AppProperties properties
    ) {
        this.restClient = builder.baseUrl(properties.getOllama().getBaseUrl()).build();
        this.properties = properties;
    }

    public String chat(List<AssistantMessage> messages, String model) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model == null || model.isBlank() ? properties.getOllama().getModel() : model);
        request.put("messages", messages);
        request.put("stream", false);

        try {
            JsonNode root = restClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(JsonNode.class);

            JsonNode contentNode = root.path("message").path("content");
            if (contentNode.isMissingNode()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Ollama returned invalid response");
            }

            return contentNode.asText();
        } catch (RestClientException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Ollama is unavailable", exception);
        }
    }
}
