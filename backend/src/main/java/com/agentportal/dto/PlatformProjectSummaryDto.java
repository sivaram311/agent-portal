package com.agentportal.dto;

public record PlatformProjectSummaryDto(
        String projectSlug,
        long taskCount,
        long openCount,
        long doneCount,
        long blockedCount,
        long linkedSessions,
        String pipelineId
) {
}
