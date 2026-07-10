package com.agentportal.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record GuidanceDefaultsRequest(
        @NotNull List<UUID> enabledPackIds
) {
}
