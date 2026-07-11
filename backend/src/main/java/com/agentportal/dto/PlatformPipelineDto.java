package com.agentportal.dto;

import java.util.List;

public record PlatformPipelineDto(
        String id,
        String name,
        String description,
        List<String> steps,
        String category,
        Integer maxIterations,
        boolean looping
) {
    public static PlatformPipelineDto standard(String id, String name, String description, List<String> steps) {
        return new PlatformPipelineDto(id, name, description, steps, "STANDARD", null, false);
    }

    public static PlatformPipelineDto systemLoop(
            String id,
            String name,
            String description,
            List<String> steps,
            int maxIterations
    ) {
        return new PlatformPipelineDto(id, name, description, steps, "SYSTEM", maxIterations, true);
    }
}
