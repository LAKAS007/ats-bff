package com.diploma.atsbff.assistant;

import java.util.List;
import java.util.Map;

public record AssistantQueryResponse(
    String content,
    String asset,
    List<String> toolsUsed,
    Map<String, Object> marketSnapshot,
    Map<String, Object> analysis
) {
}
