package com.diploma.atsbff.assistant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AssistantQueryRequest(
    @NotEmpty List<@Valid AssistantMessage> messages,
    Boolean includeMarketContext,
    String model
) {
}
