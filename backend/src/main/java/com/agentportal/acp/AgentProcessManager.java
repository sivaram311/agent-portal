package com.agentportal.acp;

import com.agentportal.config.AgentProperties;
import com.agentportal.domain.AgentSession;
import com.agentportal.repo.*;
import com.agentportal.service.AntigravityCapabilityService;
import com.agentportal.service.SessionEventBus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Routes portal sessions to Cursor ACP or Antigravity runtimes.
 */
@Component
public class AgentProcessManager {

    private static final Logger log = LoggerFactory.getLogger(AgentProcessManager.class);

    private final Map<UUID, SessionAgentRuntime> runtimes = new ConcurrentHashMap<>();
    private final AgentProperties properties;
    private final ObjectMapper mapper;
    private final SessionEventBus eventBus;
    private final AgentSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final AgentEventRepository eventRepository;
    private final ToolRunRepository toolRunRepository;
    private final PermissionRequestRepository permissionRepository;
    private final AntigravityCapabilityService antigravityCapabilityService;

    public AgentProcessManager(
            AgentProperties properties,
            ObjectMapper mapper,
            SessionEventBus eventBus,
            AgentSessionRepository sessionRepository,
            ChatMessageRepository messageRepository,
            AgentEventRepository eventRepository,
            ToolRunRepository toolRunRepository,
            PermissionRequestRepository permissionRepository,
            AntigravityCapabilityService antigravityCapabilityService
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
    }

    public SessionAgentRuntime getOrStart(AgentSession session) throws Exception {
        SessionAgentRuntime existing = runtimes.get(session.getId());
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            existing = runtimes.get(session.getId());
            if (existing != null) {
                return existing;
            }
            SessionAgentRuntime runtime = createRuntime(session);
            runtime.ensureStarted();
            runtimes.put(session.getId(), runtime);
            return runtime;
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
                properties.isDefaultAutoApprove()
        );
        bridge.start();
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
    }
}
