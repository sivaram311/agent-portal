package com.agentportal.service;

import com.agentportal.acp.AgentProcessManager;
import com.agentportal.acp.SessionAgentRuntime;
import com.agentportal.config.AgentProperties;
import com.agentportal.config.CssProperties;
import com.agentportal.domain.*;
import com.agentportal.dto.*;
import com.agentportal.repo.*;
import com.agentportal.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class SessionService {

    private final AgentSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ToolRunRepository toolRunRepository;
    private final PermissionRequestRepository permissionRepository;
    private final AgentProcessManager processManager;
    private final AgentProperties agentProperties;
    private final WorkspaceFileService workspaceFileService;
    private final AuditService auditService;
    private final CssProperties cssProperties;

    public SessionService(
            AgentSessionRepository sessionRepository,
            ChatMessageRepository messageRepository,
            ToolRunRepository toolRunRepository,
            PermissionRequestRepository permissionRepository,
            AgentEventRepository eventRepository,
            AgentProcessManager processManager,
            AgentProperties agentProperties,
            WorkspaceFileService workspaceFileService,
            AuditService auditService,
            CssProperties cssProperties
    ) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.toolRunRepository = toolRunRepository;
        this.permissionRepository = permissionRepository;
        this.processManager = processManager;
        this.agentProperties = agentProperties;
        this.workspaceFileService = workspaceFileService;
        this.auditService = auditService;
        this.cssProperties = cssProperties;
    }

    @Transactional
    public SessionDto create(CreateSessionRequest request) {
        Path workspace = resolveWorkspace(request.workspacePath());
        try {
            Files.createDirectories(workspace);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create workspace: " + e.getMessage(), e);
        }

        String provider = AgentProcessManager.normalizeProvider(request.provider());

        AgentSession session = new AgentSession();
        String title = request.title();
        if (title == null || title.isBlank()) {
            title = "Session " + Instant.now();
        }
        session.setTitle(title.trim());
        session.setWorkspacePath(workspace.toString());
        session.setStatus(SessionStatus.IDLE);
        session.setProvider(provider);
        session.setOwnerUsername(CurrentUser.usernameOrAnonymous());
        session = sessionRepository.save(session);
        auditService.record("session.create", session.getId().toString(), provider + " @ " + workspace);
        return SessionDto.from(session);
    }

    public List<SessionDto> list() {
        if (enforceOwnership()) {
            String user = CurrentUser.usernameOrAnonymous();
            if (CurrentUser.isAdmin()) {
                return sessionRepository.findByStatusNotOrderByUpdatedAtDesc(SessionStatus.ARCHIVED)
                        .stream()
                        .map(SessionDto::from)
                        .toList();
            }
            return sessionRepository
                    .findByOwnerUsernameAndStatusNotOrderByUpdatedAtDesc(user, SessionStatus.ARCHIVED)
                    .stream()
                    .map(SessionDto::from)
                    .toList();
        }
        return sessionRepository.findByStatusNotOrderByUpdatedAtDesc(SessionStatus.ARCHIVED)
                .stream()
                .map(SessionDto::from)
                .toList();
    }

    public SessionDto get(UUID id) {
        return SessionDto.from(require(id));
    }

    public List<MessageDto> messages(UUID id) {
        require(id);
        return messageRepository.findBySessionIdOrderBySequenceNoAsc(id).stream().map(MessageDto::from).toList();
    }

    public List<ToolRunDto> toolRuns(UUID id) {
        require(id);
        return toolRunRepository.findBySessionIdOrderByStartedAtAsc(id).stream().map(ToolRunDto::from).toList();
    }

    public List<PermissionDto> pendingPermissions(UUID id) {
        require(id);
        return permissionRepository.findBySessionIdAndStatusOrderByCreatedAtDesc(id, PermissionStatus.PENDING)
                .stream()
                .map(PermissionDto::from)
                .toList();
    }

    @Transactional
    public MessageDto prompt(UUID id, PromptRequest request) throws Exception {
        AgentSession session = require(id);
        if (session.getStatus() == SessionStatus.ARCHIVED) {
            throw new IllegalStateException("Session is archived");
        }
        boolean softAgyFollowUp = "antigravity".equalsIgnoreCase(session.getProvider())
                && session.getStatus() == SessionStatus.WAITING_PERMISSION;
        if (!softAgyFollowUp && (session.getStatus() == SessionStatus.STREAMING
                || session.getStatus() == SessionStatus.WAITING_PERMISSION
                || session.getStatus() == SessionStatus.WAITING_PLAN)) {
            throw new IllegalStateException("Session already has an active run");
        }

        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(id);
        userMsg.setRole(MessageRole.USER);
        userMsg.setContent(request.prompt());
        userMsg.setSequenceNo(nextSequence(id));
        messageRepository.save(userMsg);

        SessionAgentRuntime runtime = processManager.getOrStart(session);
        runtime.prompt(request.prompt());
        auditService.record("session.prompt", id.toString(), "len=" + request.prompt().length());
        return MessageDto.from(userMsg);
    }

    @Transactional
    public void cancel(UUID id) {
        AgentSession session = require(id);
        SessionAgentRuntime runtime = processManager.get(session.getId());
        if (runtime != null) {
            runtime.cancel();
        } else {
            session.setStatus(SessionStatus.CANCELLED);
            sessionRepository.save(session);
        }
        auditService.record("session.cancel", id.toString(), null);
    }

    @Transactional
    public Map<String, Object> abandonSubagent(UUID sessionId, String subagentId) {
        AgentSession session = require(sessionId);
        ToolRun run = toolRunRepository.findBySessionIdAndToolCallId(sessionId, subagentId)
                .or(() -> toolRunRepository.findBySessionIdOrderByStartedAtAsc(sessionId).stream()
                        .filter(t -> subagentId.equals(t.getSubagentId()) || subagentId.equals(t.getId().toString()))
                        .findFirst())
                .orElseThrow(() -> new NoSuchElementException("Sub-agent/tool not found: " + subagentId));

        run.setStatus("abandoned");
        run.setFinishedAt(Instant.now());
        toolRunRepository.save(run);

        SessionAgentRuntime runtime = processManager.get(session.getId());
        boolean childOnly = false;
        if (runtime != null) {
            childOnly = runtime.abandonSubagent(subagentId);
            if (!childOnly) {
                runtime.cancel();
            }
        }
        auditService.record(
                "subagent.abandon",
                sessionId.toString(),
                subagentId + " childOnly=" + childOnly
        );
        return Map.of(
                "status", "abandoned",
                "subagentId", subagentId,
                "toolRunId", run.getId().toString(),
                "sessionCancelled", !childOnly,
                "message", childOnly
                        ? "Child abandoned"
                        : "Provider cannot abandon a single child; session run was cancelled"
        );
    }

    public List<FileEntryDto> listFiles(UUID sessionId, String path) throws Exception {
        AgentSession session = require(sessionId);
        return workspaceFileService.list(Path.of(session.getWorkspacePath()), path);
    }

    public FileContentDto readFile(UUID sessionId, String path) throws Exception {
        AgentSession session = require(sessionId);
        return workspaceFileService.read(Path.of(session.getWorkspacePath()), path);
    }

    public void resolvePermission(UUID sessionId, UUID permissionId, PermissionDecisionRequest request) throws Exception {
        AgentSession session = require(sessionId);
        if ("antigravity".equalsIgnoreCase(session.getProvider())) {
            throw new IllegalStateException(
                    "Antigravity sessions do not support mid-turn permission prompts in this version.");
        }
        SessionAgentRuntime runtime = processManager.getOrStart(session);
        runtime.resolvePermission(permissionId, request.decision(), request.reason());
    }

    @Transactional
    public SessionDto archive(UUID id) {
        AgentSession session = require(id);
        processManager.stop(id);
        session.setStatus(SessionStatus.ARCHIVED);
        auditService.record("session.archive", id.toString(), null);
        return SessionDto.from(sessionRepository.save(session));
    }

    /** Used by STOMP interceptor — returns true if caller may watch this session. */
    public boolean canAccess(UUID id) {
        try {
            require(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private AgentSession require(UUID id) {
        AgentSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Session not found: " + id));
        assertOwner(session);
        return session;
    }

    private void assertOwner(AgentSession session) {
        if (!enforceOwnership()) {
            return;
        }
        if (CurrentUser.isAdmin()) {
            return;
        }
        String user = CurrentUser.usernameOrAnonymous();
        String owner = session.getOwnerUsername();
        if (owner == null || owner.isBlank()) {
            // Legacy rows: only visible when auth is off, or to admin (handled above).
            throw new NoSuchElementException("Session not found: " + session.getId());
        }
        if (!owner.equals(user)) {
            throw new NoSuchElementException("Session not found: " + session.getId());
        }
    }

    private boolean enforceOwnership() {
        return cssProperties.isEnabled();
    }

    private int nextSequence(UUID sessionId) {
        return (int) (messageRepository.maxSequence(sessionId) + 1);
    }

    /**
     * Relative paths resolve under agent.workspace.root.
     * Absolute paths are allowed only when they stay under that root.
     */
    private Path resolveWorkspace(String requested) {
        Path root = Path.of(agentProperties.getWorkspace().getRoot()).toAbsolutePath().normalize();
        if (requested == null || requested.isBlank()) {
            throw new IllegalArgumentException("workspacePath is required");
        }
        String trimmed = requested.trim();
        Path candidate;
        if (trimmed.contains("..")) {
            throw new SecurityException("workspacePath must not contain '..'");
        }
        boolean absolute = Path.of(trimmed).isAbsolute()
                || trimmed.contains(":")
                || trimmed.startsWith("/")
                || trimmed.startsWith("\\");
        if (absolute) {
            candidate = Path.of(trimmed).toAbsolutePath().normalize();
        } else {
            candidate = root.resolve(trimmed).toAbsolutePath().normalize();
        }
        if (!candidate.startsWith(root)) {
            throw new SecurityException("workspacePath must stay under " + root);
        }
        return candidate;
    }
}
