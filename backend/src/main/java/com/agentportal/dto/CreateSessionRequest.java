package com.agentportal.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSessionRequest(
        String title,
        @NotBlank String workspacePath,
        String provider
) {
}
