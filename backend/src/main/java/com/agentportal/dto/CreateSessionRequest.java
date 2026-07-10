package com.agentportal.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSessionRequest(
        String title,
        @NotBlank String workspacePath,
        String provider,
        /** When true (default), copy the user's enabled-by-default guidance packs onto the session. */
        Boolean useGuidanceDefaults
) {
}
