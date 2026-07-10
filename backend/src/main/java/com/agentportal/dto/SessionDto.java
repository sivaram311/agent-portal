package com.agentportal.dto;

import com.agentportal.domain.AgentSession;
import com.agentportal.domain.SessionStatus;

import java.time.Instant;
import java.util.UUID;

public record SessionDto(
        UUID id,
        String title,
        String workspacePath,
        String cursorSessionId,
        SessionStatus status,
        String provider,
        String ownerUsername,
        Instant createdAt,
        Instant updatedAt
) {
    public static SessionDto from(AgentSession s) {
        return new SessionDto(
                s.getId(),
                s.getTitle(),
                s.getWorkspacePath(),
                s.getCursorSessionId(),
                s.getStatus(),
                s.getProvider(),
                s.getOwnerUsername(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
