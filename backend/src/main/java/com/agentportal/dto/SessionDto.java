package com.agentportal.dto;

import com.agentportal.domain.AgentSession;
import com.agentportal.domain.SessionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SessionDto(
        UUID id,
        String title,
        String workspacePath,
        String cursorSessionId,
        SessionStatus status,
        String provider,
        String ownerUsername,
        String platformRole,
        UUID platformTaskId,
        List<String> allowedTools,
        List<String> allowedActions,
        String rolePromptHint,
        Boolean humanApprovalRequired,
        Instant createdAt,
        Instant updatedAt
) {
    public static SessionDto from(AgentSession s) {
        return from(s, null);
    }

    public static SessionDto from(AgentSession s, PlatformRoleDto role) {
        return new SessionDto(
                s.getId(),
                s.getTitle(),
                s.getWorkspacePath(),
                s.getCursorSessionId(),
                s.getStatus(),
                s.getProvider(),
                s.getOwnerUsername(),
                s.getPlatformRole(),
                s.getPlatformTaskId(),
                role == null ? List.of() : role.allowedTools(),
                role == null ? List.of() : role.allowedActions(),
                role == null ? null : role.promptHint(),
                role == null ? null : role.humanApprovalRequired(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
