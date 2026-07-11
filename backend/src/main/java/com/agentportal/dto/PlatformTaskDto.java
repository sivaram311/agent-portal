package com.agentportal.dto;

import com.agentportal.domain.PlatformTask;

import java.time.Instant;
import java.util.UUID;

public record PlatformTaskDto(
        UUID id,
        String title,
        String description,
        String role,
        String status,
        String projectSlug,
        String workspacePath,
        String createdBy,
        String assigneeUsername,
        UUID sessionId,
        Instant createdAt,
        Instant updatedAt
) {
    public static PlatformTaskDto from(PlatformTask t) {
        return new PlatformTaskDto(
                t.getId(),
                t.getTitle(),
                t.getDescription(),
                t.getRole(),
                t.getStatus(),
                t.getProjectSlug(),
                t.getWorkspacePath(),
                t.getCreatedBy(),
                t.getAssigneeUsername(),
                t.getSessionId(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
