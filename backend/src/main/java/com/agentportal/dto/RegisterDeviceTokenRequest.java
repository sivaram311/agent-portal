package com.agentportal.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterDeviceTokenRequest(@NotBlank String token, String platform) {
}
