package com.agentportal.dto;

public record UpdateGuidancePackRequest(
        String title,
        String description,
        String bodyMarkdown,
        String globs,
        Boolean alwaysApply,
        Boolean enabledByDefault,
        String slug
) {
}
