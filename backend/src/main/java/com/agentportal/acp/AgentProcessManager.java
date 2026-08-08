package com.agentportal.acp;

import com.agentportal.config.AgentProperties;
import com.agentportal.domain.AgentSession;
import com.agentportal.repo.*;
import com.agentportal.service.AntigravityCapabilityService;
import com.agentportal.service.RoleAclService;
import com.agentportal.service.SessionEventBus;
import com.agentportal.machine.MachineToolGuard;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Routes portal sessions to Cursor ACP or Antigravity runtimes.
 */
@Component
public class AgentProcessManager {

    private static final Logger log = LoggerFactory.getLogger(AgentProcessManager.class);

    private final Map<UUID, SessionAgentRuntime> runtimes = new ConcurrentHashMap<>();
    /** Per-session start locks — avoid a global monitor that serializes every ACP cold start. */
    private final ConcurrentHashMap<UUID, Object> startLocks = new ConcurrentHashMap<>();
    /**
     * Runs createRuntime() off the HTTP request thread so the outer watchdog in getOrStart can
     * give up on a hard deadline even if the start path is stuck in a call that can't be
     * cancelled (e.g. ProcessBuilder.start() itself hanging on a slow/AV-scanned Windows spawn).
     * Cached pool: cold starts are infrequent and per-session-serialized, so unbounded growth
     * isn't a real risk, and idle workers time out after 60s.
     */
    private final ExecutorService startExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "acp-start-watchdog");
        t.setDaemon(true);
        return t;
    });
    private final AgentProperties properties;
    private final ObjectMapper mapper;
    private final SessionEventBus eventBus;
    private final AgentSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final AgentEventRepository eventRepository;
    private final ToolRunRepository toolRunRepository;
    private final PermissionRequestRepository permissionRepository;
    private final AntigravityCapabilityService antigravityCapabilityService;
    private final RoleAclService roleAclService;
    private final MachineToolGuard machineToolGuard;

    public AgentProcessManager(
            AgentProperties properties,
            ObjectMapper mapper,
            SessionEventBus eventBus,
            AgentSessionRepository sessionRepository,
            ChatMessageRepository messageRepository,
            AgentEventRepository eventRepository,
            ToolRunRepository toolRunRepository,
            PermissionRequestRepository permissionRepository,
            AntigravityCapabilityService antigravityCapabilityService,
            RoleAclService roleAclService,
            MachineToolGuard machineToolGuard
    ) {
        this.properties = properties;
        this.mapper = mapper;
        this.eventBus = eventBus;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.eventRepository = eventRepository;
        this.toolRunRepository = toolRunRepository;
        this.permissionRepository = permissionRepository;
        this.antigravityCapabilityService = antigravityCapabilityService;
        this.roleAclService = roleAclService;
        this.machineToolGuard = machineToolGuard;
    }

    public SessionAgentRuntime getOrStart(AgentSession session) throws Exception {
        UUID sessionId = session.getId();
        SessionAgentRuntime existing = runtimes.get(sessionId);
        if (existing != null && existing.isHealthy()) {
            log.info("ACP reuse healthy runtime session={}", sessionId);
            return existing;
        }
        if (existing != null) {
            log.warn("ACP runtime unhealthy for session {} — stopping before restart", sessionId);
            stop(sessionId);
        }

        Object lock = startLocks.computeIfAbsent(sessionId, id -> new Object());
        synchronized (lock) {
            existing = runtimes.get(sessionId);
            if (existing != null && existing.isHealthy()) {
                log.info("ACP reuse healthy runtime session={} (after wait)", sessionId);
                return existing;
            }
            if (existing != null) {
                stop(sessionId);
            }

            long t0 = System.currentTimeMillis();
            long hardBudgetMs = TimeUnit.SECONDS.toMillis(
                    properties.getCursor().getStartTimeoutSeconds()
                            + properties.getCursor().getStartWatchdogBufferSeconds()
            );
            log.info(
                    "ACP getOrStart begin session={} provider={} budgetSec={} hardWatchdogMs={}",
                    sessionId,
                    normalizeProvider(session.getProvider()),
                    properties.getCursor().getStartTimeoutSeconds(),
                    hardBudgetMs
            );

            CompletableFuture<SessionAgentRuntime> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return createRuntime(session);
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            }, startExecutor);

            SessionAgentRuntime runtime = null;
            try {
                runtime = future.get(hardBudgetMs, TimeUnit.MILLISECONDS);
                runtime.ensureStarted();
                runtimes.put(sessionId, runtime);
                log.info(
                        "ACP getOrStart ok session={} mode=started waitedMs={}",
                        sessionId,
                        System.currentTimeMillis() - t0
                );
                return runtime;
            } catch (IllegalStateException ise) {
                // future.get() succeeded but ensureStarted() rejected the runtime — close it,
                // don't leave it dangling outside the runtimes map.
                log.warn(
                        "ACP getOrStart ensureStarted failed session={} waitedMs={}: {}",
                        sessionId,
                        System.currentTimeMillis() - t0,
                        ise.getMessage()
                );
                if (runtime != null) {
                    try {
                        runtime.close();
                    } catch (Exception closeEx) {
                        log.debug("Failed closing runtime after ensureStarted error: {}", closeEx.getMessage());
                    }
                }
                throw ise;
            } catch (TimeoutException te) {
                long waitedMs = System.currentTimeMillis() - t0;
                log.warn(
                        "ACP getOrStart HARD TIMEOUT session={} waitedMs={} budgetMs={} — "
                                + "start path did not return control in time, failing request now; "
                                + "any late-arriving process will be force-closed in the background",
                        sessionId,
                        waitedMs,
                        hardBudgetMs
                );
                // The worker thread may be wedged in a call we cannot interrupt (e.g. native
                // Process creation). Don't block the caller waiting for it — instead, close
                // whatever it eventually produces so a late-starting process doesn't zombie.
                future.whenComplete((rt, err) -> {
                    if (rt != null) {
                        log.warn(
                                "ACP start for session {} completed AFTER hard watchdog timeout "
                                        + "({}ms after giving up) — force-closing orphaned runtime",
                                sessionId,
                                System.currentTimeMillis() - t0
                        );
                        try {
                            rt.close();
                        } catch (Exception closeEx) {
                            log.debug("Failed closing late/orphaned runtime: {}", closeEx.getMessage());
                        }
                    }
                });
                throw new AcpStartTimeoutException(sessionId, waitedMs, hardBudgetMs, "getOrStart(hard-watchdog)", te);
            } catch (ExecutionException ee) {
                Throwable cause = ee.getCause() != null ? ee.getCause() : ee;
                if (cause instanceof CompletionException && cause.getCause() != null) {
                    cause = cause.getCause();
                }
                log.warn(
                        "ACP getOrStart failed session={} waitedMs={}: {}",
                        sessionId,
                        System.currentTimeMillis() - t0,
                        cause.getMessage()
                );
                if (cause instanceof Exception ex) {
                    throw ex;
                }
                throw new IllegalStateException(cause);
            } finally {
                startLocks.remove(sessionId, lock);
            }
        }
    }

    public SessionAgentRuntime get(UUID sessionId) {
        return runtimes.get(sessionId);
    }

    public void stop(UUID sessionId) {
        SessionAgentRuntime runtime = runtimes.remove(sessionId);
        if (runtime != null) {
            runtime.close();
        }
    }

    private SessionAgentRuntime createRuntime(AgentSession session) throws Exception {
        String provider = normalizeProvider(session.getProvider());
        if ("antigravity".equals(provider)) {
            if (shouldUseAntigravityAcp()) {
                Exception last = null;
                for (String sub : List.of("acp", "--acp")) {
                    try {
                        AgentBridge bridge = new AgentBridge(
                                session.getId(),
                                session.getWorkspacePath(),
                                session.getCursorSessionId(),
                                properties,
                                mapper,
                                eventBus,
                                sessionRepository,
                                messageRepository,
                                eventRepository,
                                toolRunRepository,
                                permissionRepository,
                                roleAclService,
                                machineToolGuard,
                                properties.isDefaultAutoApprove(),
                                properties.getAntigravity().getCommand(),
                                sub
                        );
                        bridge.start();
                        log.info("Antigravity session {} using ACP mode ({})", session.getId(), sub);
                        return CursorSessionRuntime.fromBridge(bridge);
                    } catch (Exception e) {
                        last = e;
                        log.warn("Antigravity ACP launch with '{}' failed: {}", sub, e.getMessage());
                    }
                }
                if (last != null) {
                    log.warn("Antigravity ACP unavailable, falling back to print-mode: {}", last.getMessage());
                }
            }
            return new AntigravityBridge(
                    session.getId(),
                    session.getWorkspacePath(),
                    session.getCursorSessionId(),
                    properties,
                    mapper,
                    eventBus,
                    sessionRepository,
                    messageRepository,
                    eventRepository,
                    toolRunRepository
            );
        }

        AgentBridge bridge = new AgentBridge(
                session.getId(),
                session.getWorkspacePath(),
                session.getCursorSessionId(),
                properties,
                mapper,
                eventBus,
                sessionRepository,
                messageRepository,
                eventRepository,
                toolRunRepository,
                permissionRepository,
                roleAclService,
                machineToolGuard,
                properties.isDefaultAutoApprove(),
                null,
                null
        );
        try {
            bridge.start();
        } catch (Exception e) {
            bridge.close();
            throw e;
        }
        return CursorSessionRuntime.fromBridge(bridge);
    }

    private boolean shouldUseAntigravityAcp() {
        if (!properties.getAntigravity().isPreferAcp()) {
            return false;
        }
        String protocol = properties.getAntigravity().getInteractiveProtocol();
        if ("soft".equalsIgnoreCase(protocol) || "none".equalsIgnoreCase(protocol)) {
            return false;
        }
        Map<String, Object> caps = antigravityCapabilityService.probe();
        Object supports = caps.get("supportsAcp");
        return Boolean.TRUE.equals(supports);
    }

    public static String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return "cursor";
        }
        String p = provider.trim().toLowerCase(Locale.ROOT);
        return switch (p) {
            case "antigravity", "agy", "gemini" -> "antigravity";
            default -> "cursor";
        };
    }

    @PreDestroy
    public void shutdown() {
        runtimes.keySet().forEach(this::stop);
        startExecutor.shutdownNow();
    }
}
