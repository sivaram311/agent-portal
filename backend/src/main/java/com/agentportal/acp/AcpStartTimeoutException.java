package com.agentportal.acp;

import java.util.UUID;

/**
 * ACP process initialize/session bootstrap exceeded the configured start budget.
 */
public class AcpStartTimeoutException extends RuntimeException {

    private final UUID sessionId;
    private final long waitedMs;
    private final long timeoutMs;

    public AcpStartTimeoutException(UUID sessionId, long waitedMs, long timeoutMs, String step, Throwable cause) {
        super(
                "ACP start timed out after " + waitedMs + "ms (limit " + timeoutMs + "ms) at step '" + step
                        + "' for session " + sessionId
                        + ". Cursor CLI did not become ready in time.",
                cause
        );
        this.sessionId = sessionId;
        this.waitedMs = waitedMs;
        this.timeoutMs = timeoutMs;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public long getWaitedMs() {
        return waitedMs;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }
}
