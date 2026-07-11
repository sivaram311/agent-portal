package com.agentportal.dto;

public record PlatformRoleDto(
        String id,
        String name,
        String department,
        String defaultWorkspaceHint,
        String skillHint
) {
}
