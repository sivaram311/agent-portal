package com.agentportal.dto;

import java.util.UUID;

public record MachineChatResponse(
        UUID sessionId,
        String mode,
        String platformRole,
        String workspacePath,
        MessageDto userMessage,
        String status
) {
}
