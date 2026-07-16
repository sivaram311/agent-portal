package com.agentportal.dto;

import jakarta.validation.constraints.NotBlank;

public record MachineChatRequest(
        @NotBlank String message,
        /** observe|advise|act|ops — defaults to act (still clamped by max-mode). */
        String mode,
        /** Optional VirtualDev / gateway role override; normally derived from mode. */
        String role,
        /** cursor|antigravity — optional. */
        String provider,
        /** Reuse an existing gateway session id when set. */
        java.util.UUID sessionId
) {
}
