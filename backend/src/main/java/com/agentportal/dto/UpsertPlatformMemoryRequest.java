package com.agentportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpsertPlatformMemoryRequest(
        @NotBlank @Size(max = 128) String projectSlug,
        @NotBlank @Size(max = 200) String key,
        @Pattern(regexp = "NOTE|DECISION|CONTRACT|ARTIFACT|MESSAGE_SUMMARY|PROGRESS") String kind,
        @NotBlank String value
) {
}
