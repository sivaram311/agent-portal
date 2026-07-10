package com.agentportal.dto;

import com.agentportal.domain.PermissionRequest;
import com.agentportal.domain.PermissionStatus;

import java.time.Instant;
import java.util.UUID;

public record PermissionDto(
        UUID id,
        UUID sessionId,
        String toolCallId,
        String detailsJson,
        PermissionStatus status,
        String kind,
        String planMarkdown,
        Instant createdAt
) {
    public static PermissionDto from(PermissionRequest p) {
        return new PermissionDto(
                p.getId(),
                p.getSessionId(),
                p.getToolCallId(),
                p.getDetailsJson(),
                p.getStatus(),
                p.getKind(),
                p.getPlanMarkdown(),
                p.getCreatedAt()
        );
    }
}
