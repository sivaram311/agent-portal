package com.agentportal.dto;

import com.agentportal.domain.PortLease;

import java.time.Instant;
import java.util.UUID;

public record PortLeaseDto(
        UUID id,
        int port,
        String service,
        String ownerApp,
        String env,
        String bindAddress,
        String status,
        String claimedBy,
        String notes,
        Instant claimedAt,
        Instant releasedAt
) {
    public static PortLeaseDto from(PortLease p) {
        return new PortLeaseDto(
                p.getId(),
                p.getPort(),
                p.getService(),
                p.getOwnerApp(),
                p.getEnv(),
                p.getBindAddress(),
                p.getStatus(),
                p.getClaimedBy(),
                p.getNotes(),
                p.getClaimedAt(),
                p.getReleasedAt()
        );
    }
}
