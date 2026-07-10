package com.agentportal.acp;

import com.agentportal.domain.AgentSession;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Adapter wrapping the existing Cursor {@link AgentBridge}.
 */
public class CursorSessionRuntime implements SessionAgentRuntime {

    private final AgentBridge bridge;

    public CursorSessionRuntime(AgentBridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public void ensureStarted() {
        // AgentBridge.start() already invoked by process manager.
    }

    @Override
    public CompletableFuture<Void> prompt(String text) {
        return bridge.prompt(text);
    }

    @Override
    public void cancel() {
        bridge.cancel();
    }

    @Override
    public void resolvePermission(UUID permissionId, String decision, String reason) throws Exception {
        bridge.resolvePermission(permissionId, decision, reason);
    }

    @Override
    public void close() {
        bridge.close();
    }

    public static CursorSessionRuntime fromBridge(AgentBridge bridge) {
        return new CursorSessionRuntime(bridge);
    }

    public AgentBridge getBridge() {
        return bridge;
    }
}
