package com.agentportal.dto;

import com.agentportal.domain.PlatformApp;

import java.time.Instant;
import java.util.UUID;

public record PlatformAppDto(
        UUID id,
        String slug,
        String name,
        String clientId,
        String env,
        String baseUrl,
        String healthUrl,
        String subdomain,
        Integer upstreamPort,
        boolean enabled,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
    public static PlatformAppDto from(PlatformApp a) {
        return new PlatformAppDto(
                a.getId(),
                a.getSlug(),
                a.getName(),
                a.getClientId(),
                a.getEnv(),
                a.getBaseUrl(),
                a.getHealthUrl(),
                a.getSubdomain(),
                a.getUpstreamPort(),
                a.isEnabled(),
                a.getDescription(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
