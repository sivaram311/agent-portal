package com.agentportal.dto;

import java.util.List;
import java.util.UUID;

public record SwarmTickResultDto(
        String projectSlug,
        int advanced,
        int parentsCompleted,
        int messagesSent,
        List<SwarmActionDto> actions
) {
    public record SwarmActionDto(
            String type,
            UUID taskId,
            String role,
            String detail
    ) {
    }
}
