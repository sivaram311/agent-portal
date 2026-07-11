package com.agentportal.dto;

import jakarta.validation.constraints.Pattern;

public record UpdatePlatformAgentMessageRequest(
        @Pattern(regexp = "UNREAD|READ") String status
) {
}
