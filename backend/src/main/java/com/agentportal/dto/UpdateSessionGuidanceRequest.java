package com.agentportal.dto;

import java.util.List;

public record UpdateSessionGuidanceRequest(
        List<SessionGuidanceEntryRequest> items
) {
}
