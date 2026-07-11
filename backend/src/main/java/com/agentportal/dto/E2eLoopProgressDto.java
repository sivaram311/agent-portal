package com.agentportal.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record E2eLoopProgressDto(
        UUID runId,
        String projectSlug,
        String pipelineId,
        String status,
        int iteration,
        int maxIterations,
        String lastQaOutcome,
        String stopReason,
        List<StepDto> steps,
        Instant updatedAt
) {
    public record StepDto(
            UUID taskId,
            int iteration,
            String stepKey,
            String role,
            String status,
            String outcome,
            String title,
            Instant updatedAt
    ) {
    }
}
