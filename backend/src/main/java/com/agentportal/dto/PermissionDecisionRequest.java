package com.agentportal.dto;

import jakarta.validation.constraints.NotBlank;

public record PermissionDecisionRequest(
        @NotBlank String decision,
        String reason
) {
}
