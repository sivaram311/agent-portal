package com.agentportal.dto;

import com.agentportal.domain.GuidanceKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateGuidancePackRequest(
        @NotNull GuidanceKind kind,
        @NotBlank String title,
        String description,
        @NotBlank String bodyMarkdown,
        String globs,
        Boolean alwaysApply,
        Boolean enabledByDefault,
        String slug
) {
}
