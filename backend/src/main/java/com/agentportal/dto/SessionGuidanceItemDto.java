package com.agentportal.dto;

import com.agentportal.domain.GuidanceKind;

import java.util.UUID;

public record SessionGuidanceItemDto(
        UUID id,
        UUID packId,
        GuidanceKind kind,
        String title,
        String bodyMarkdown,
        boolean enabled,
        int sortOrder,
        boolean sessionOnly
) {
}
