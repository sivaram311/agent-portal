package com.agentportal.dto;

import com.agentportal.domain.PlatformMemoryEntry;

import java.time.Instant;
import java.util.UUID;

public record PlatformMemoryDto(
        UUID id,
        String projectSlug,
        String key,
        String kind,
        String value,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static PlatformMemoryDto from(PlatformMemoryEntry e) {
        return new PlatformMemoryDto(
                e.getId(),
                e.getProjectSlug(),
                e.getEntryKey(),
                e.getKind(),
                e.getValue(),
                e.getCreatedBy(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
