package com.agentportal.dto;

import java.util.Map;

/**
 * Unified Machine Gateway response: always includes redacted {@code context};
 * {@code chat} is non-null only when a message was accepted.
 */
public record MachineGatewayResponse(
        Map<String, Object> context,
        MachineChatResponse chat
) {
}
