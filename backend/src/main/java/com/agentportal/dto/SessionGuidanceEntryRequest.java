package com.agentportal.dto;

import com.agentportal.domain.GuidanceKind;

import java.util.UUID;

public record SessionGuidanceEntryRequest(
        UUID packId,
        Boolean enabled,
        Integer sortOrder,
        String title,
        String sessionBody,
        GuidanceKind kind
) {
}
