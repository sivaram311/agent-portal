package com.agentportal.dto;

import java.util.List;

public record PlatformRoleDto(
        String id,
        String name,
        String department,
        String defaultWorkspaceHint,
        String skillHint,
        List<String> allowedTools,
        List<String> allowedActions,
        String promptHint,
        boolean humanApprovalRequired
) {
}
