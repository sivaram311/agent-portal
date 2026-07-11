package com.agentportal.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdatePlatformTaskRequest(
        @Size(max = 200) String title,
        String description,
        @Pattern(regexp = "ARCHITECTURE|BACKEND|FRONTEND|QA|DEVOPS|PRODUCT|SECURITY") String role,
        @Pattern(regexp = "OPEN|ASSIGNED|IN_PROGRESS|BLOCKED|DONE|CANCELLED") String status,
        @Size(max = 128) String projectSlug,
        @Size(max = 1024) String workspacePath,
        @Size(max = 128) String assigneeUsername,
        UUID sessionId,
        UUID parentTaskId,
        @Size(max = 64) String pipelineId
) {
}
