package com.agentportal.dto;

public record FileChangeDto(
        String path,
        String status,
        long size,
        String unifiedDiff,
        String source
) {
}
