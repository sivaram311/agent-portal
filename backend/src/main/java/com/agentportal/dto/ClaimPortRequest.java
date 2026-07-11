package com.agentportal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClaimPortRequest(
        @NotNull @Min(1) @Max(65535) Integer port,
        @NotBlank String service,
        @NotBlank String ownerApp,
        String env,
        String bindAddress,
        String notes
) {}
