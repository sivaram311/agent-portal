package com.agentportal.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LinkTaskSessionRequest(
        @NotNull UUID sessionId
) {
}
