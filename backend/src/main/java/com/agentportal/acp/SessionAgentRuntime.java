package com.agentportal.acp;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Runtime handle for one portal session's agent engine (Cursor ACP or Antigravity).
 */
public interface SessionAgentRuntime extends AutoCloseable {

    void ensureStarted() throws Exception;

    CompletableFuture<Void> prompt(String text);

    void cancel();

    void resolvePermission(UUID permissionId, String decision, String reason) throws Exception;

    @Override
    void close();
}
