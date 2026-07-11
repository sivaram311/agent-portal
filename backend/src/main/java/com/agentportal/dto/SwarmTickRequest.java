package com.agentportal.dto;

import jakarta.validation.constraints.Size;

public record SwarmTickRequest(
        @Size(max = 128) String projectSlug
) {
}
