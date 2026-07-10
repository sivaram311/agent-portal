package com.agentportal.acp;

import com.agentportal.domain.AgentSession;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Marker bean documenting Antigravity as a first-class provider.
 * Runtime work is handled by {@link AntigravityBridge} via {@link AgentProcessManager}.
 */
@Component
public class AntigravityAgentProvider implements AgentProvider {

    @Override
    public String id() {
        return "antigravity";
    }

    @Override
    public void start(AgentSession session) {
        // Lifecycle owned by AgentProcessManager / AntigravityBridge.
    }

    @Override
    public CompletableFuture<Void> prompt(UUID sessionId, String text) {
        return CompletableFuture.failedFuture(
                new UnsupportedOperationException("Use SessionService.prompt — routed by AgentProcessManager"));
    }

    @Override
    public void cancel(UUID sessionId) {
        // no-op marker
    }

    @Override
    public void resolvePermission(UUID sessionId, UUID permissionId, String decision, String reason) {
        throw new UnsupportedOperationException(
                "Antigravity mid-turn permissions are not supported; skip-permissions is used for portal runs.");
    }

    @Override
    public void stop(UUID sessionId) {
        // no-op marker
    }
}
