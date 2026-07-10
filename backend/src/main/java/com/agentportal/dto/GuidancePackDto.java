package com.agentportal.dto;

import com.agentportal.domain.GuidanceKind;
import com.agentportal.domain.GuidancePack;

import java.time.Instant;
import java.util.UUID;

public record GuidancePackDto(
        UUID id,
        GuidanceKind kind,
        String slug,
        String title,
        String description,
        String bodyMarkdown,
        String globs,
        boolean alwaysApply,
        boolean enabledByDefault,
        Instant createdAt,
        Instant updatedAt
) {
    public static GuidancePackDto from(GuidancePack p) {
        return new GuidancePackDto(
                p.getId(),
                p.getKind(),
                p.getSlug(),
                p.getTitle(),
                p.getDescription(),
                p.getBodyMarkdown(),
                p.getGlobs(),
                p.isAlwaysApply(),
                p.isEnabledByDefault(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
