package com.diploma.atsbff.assistant;

import jakarta.validation.constraints.NotBlank;

public record AssistantMessage(
    @NotBlank String role,
    @NotBlank String content
) {
}
