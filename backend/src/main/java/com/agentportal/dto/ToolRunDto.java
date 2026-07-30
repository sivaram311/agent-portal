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
        String kind,
        String parentToolCallId,
        String subagentId,
        String output,
        Integer exitCode,
        Instant startedAt,
        Instant finishedAt
) {
    /** Cap for list endpoints so mobile/clients aren't OOMed by huge tool payloads. */
    private static final int COMPACT_FIELD_CHARS = 2_048;

    public static ToolRunDto from(ToolRun t) {
        return from(t, false);
    }

    public static ToolRunDto from(ToolRun t, boolean compact) {
        return new ToolRunDto(
                t.getId(),
                t.getSessionId(),
                t.getToolCallId(),
                t.getToolName(),
                compact ? truncate(t.getArgsJson(), COMPACT_FIELD_CHARS) : t.getArgsJson(),
                t.getStatus(),
                t.getKind() == null ? "tool" : t.getKind(),
                t.getParentToolCallId(),
                t.getSubagentId(),
                compact ? truncate(t.getOutput(), COMPACT_FIELD_CHARS) : t.getOutput(),
                t.getExitCode(),
                t.getStartedAt(),
                t.getFinishedAt()
        );
    }

    private static String truncate(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars) + "…[truncated]";
    }
}
