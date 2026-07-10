package com.agentportal.acp;

import com.agentportal.domain.AgentSession;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Pluggable agent engine. Cursor ACP is the v1 implementation;
 * Antigravity can be added later behind the same interface.
 */
public interface AgentProvider {

    String id();

    void start(AgentSession session) throws Exception;

    CompletableFuture<Void> prompt(UUID sessionId, String text);

    void cancel(UUID sessionId);

    void resolvePermission(UUID sessionId, UUID permissionId, String decision, String reason) throws Exception;

    void stop(UUID sessionId);
}
