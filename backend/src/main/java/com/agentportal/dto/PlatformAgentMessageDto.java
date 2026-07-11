package com.agentportal.dto;

import com.agentportal.domain.PlatformAgentMessage;

import java.time.Instant;
import java.util.UUID;

public record PlatformAgentMessageDto(
        UUID id,
        String projectSlug,
        UUID taskId,
        String fromRole,
        String toRole,
        String subject,
        String body,
        String status,
        String createdBy,
        Instant createdAt
) {
    public static PlatformAgentMessageDto from(PlatformAgentMessage m) {
        return new PlatformAgentMessageDto(
                m.getId(),
                m.getProjectSlug(),
                m.getTaskId(),
                m.getFromRole(),
                m.getToRole(),
                m.getSubject(),
                m.getBody(),
                m.getStatus(),
                m.getCreatedBy(),
                m.getCreatedAt()
        );
    }
}
