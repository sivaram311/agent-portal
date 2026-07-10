package com.agentportal.dto;

import java.util.List;

public record SessionGuidanceDto(
        List<SessionGuidanceItemDto> items,
        List<SessionGuidanceItemDto> effective
) {
}
