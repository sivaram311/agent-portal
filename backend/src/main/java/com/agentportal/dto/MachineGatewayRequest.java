package com.agentportal.dto;

import java.util.UUID;

/**
 * Unified Machine Gateway request. {@code message} is optional:
 * omit/blank → context only; present → context + chat accept.
 */
public record MachineGatewayRequest(
        String message,
        /** observe|advise|act|ops — defaults to act (still clamped by max-mode). */
        String mode,
        /** Optional VirtualDev / gateway role override; normally derived from mode. */
        String role,
        /** cursor|antigravity — optional. */
        String provider,
        /** Reuse an existing gateway session id when set. */
        UUID sessionId
) {
}
