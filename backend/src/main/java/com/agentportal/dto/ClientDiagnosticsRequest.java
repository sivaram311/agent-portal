package com.agentportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Mobile client diagnostics payload (Agent Portal Extended).
 */
public record ClientDiagnosticsRequest(
        @NotBlank @Size(max = 128) String deviceId,
        @Size(max = 64) String appVersion,
        Integer versionCode,
        @Size(max = 32) String platform,
        @Size(max = 32) String reason,
        @Size(max = 64) String createdAt,
        /** Newline-separated log lines (preferred). */
        @Size(max = 1_500_000) String lines,
        /** Alias for lines if clients send content instead. */
        @Size(max = 1_500_000) String content
) {
    public String bodyText() {
        if (lines != null && !lines.isBlank()) {
            return lines;
        }
        return content == null ? "" : content;
    }
}
