package com.agentportal.dto;

import com.agentportal.domain.ToolRun;

import java.time.Instant;
import java.util.UUID;

public record ToolRunDto(
        UUID id,
        UUID sessionId,
        String toolCallId,
        String toolName,
        String argsJson,
        String status,
        String output,
        Integer exitCode,
        Instant startedAt,
        Instant finishedAt
) {
    public static ToolRunDto from(ToolRun t) {
        return new ToolRunDto(
                t.getId(),
                t.getSessionId(),
                t.getToolCallId(),
                t.getToolName(),
                t.getArgsJson(),
                t.getStatus(),
                t.getOutput(),
                t.getExitCode(),
                t.getStartedAt(),
                t.getFinishedAt()
        );
    }
}
