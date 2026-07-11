package com.agentportal.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateSessionRequest(
        String title,
        @NotBlank String workspacePath,
        String provider,
        /** When true (default), copy the user's enabled-by-default guidance packs onto the session. */
        Boolean useGuidanceDefaults,
        /** Optional VirtualDev role for runtime ACL + prompt hints. */
        String platformRole,
        UUID platformTaskId
) {
}
