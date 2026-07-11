package com.agentportal.dto;

import java.util.UUID;

public record UpdateSessionRoleRequest(
        String platformRole,
        UUID platformTaskId
) {
}
