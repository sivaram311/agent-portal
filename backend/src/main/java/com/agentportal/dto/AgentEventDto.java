package com.agentportal.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AgentEventDto(
        UUID sessionId,
        String type,
        Map<String, Object> payload,
        Instant timestamp
) {
}
