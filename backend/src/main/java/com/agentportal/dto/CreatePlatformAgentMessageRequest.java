package com.agentportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreatePlatformAgentMessageRequest(
        @NotBlank @Size(max = 128) String projectSlug,
        UUID taskId,
        @NotBlank @Pattern(regexp = "ARCHITECTURE|BACKEND|FRONTEND|QA|DEVOPS|PRODUCT|SECURITY|EM") String fromRole,
        @NotBlank @Pattern(regexp = "ARCHITECTURE|BACKEND|FRONTEND|QA|DEVOPS|PRODUCT|SECURITY|EM|\\*") String toRole,
        @NotBlank @Size(max = 200) String subject,
        @NotBlank String body
) {
}
