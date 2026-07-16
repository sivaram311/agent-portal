package com.agentportal.machine;

import java.util.Locale;

/**
 * Blast-radius modes for Machine Gateway chat/context.
 * Ordered lowest → highest privilege for ceiling checks.
 */
public enum MachineMode {
    OBSERVE,
    ADVISE,
    ACT,
    OPS;

    public static MachineMode parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return ACT;
        }
        return MachineMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }

    public String platformRole() {
        return "GATEWAY_" + name();
    }

    public boolean isAtMost(MachineMode ceiling) {
        return this.ordinal() <= ceiling.ordinal();
    }

    public static String roleFor(MachineMode mode) {
        return mode.platformRole();
    }
}
