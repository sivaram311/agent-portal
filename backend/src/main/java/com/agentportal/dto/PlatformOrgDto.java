package com.agentportal.dto;

import java.util.List;
import java.util.Map;

public record PlatformOrgDto(
        String title,
        Map<String, Long> tasksByStatus,
        Map<String, Long> tasksByRole,
        long unreadMessages,
        long memoryEntries,
        long linkedSessions,
        long activeProjects,
        List<PlatformProjectSummaryDto> projects,
        List<PlatformTaskDto> blockedTasks,
        List<PlatformTaskDto> recentOpenTasks,
        List<PlatformRoleDto> roles
) {
}
