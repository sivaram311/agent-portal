package com.agentportal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RunPlatformPipelineRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 128) String projectSlug,
        String description,
        @Size(max = 1024) String workspacePath,
        @Min(1) @Max(100) Integer maxIterations
) {
}
