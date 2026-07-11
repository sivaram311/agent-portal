package com.agentportal.dto;

import java.util.List;

public record PlatformPipelineDto(
        String id,
        String name,
        String description,
        List<String> steps
) {
}
