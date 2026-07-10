package com.agentportal.service;

import com.agentportal.acp.AgentProcessManager;
import com.agentportal.acp.CursorSessionRuntime;
import com.agentportal.acp.SessionAgentRuntime;
import com.agentportal.config.AgentProperties;
import com.agentportal.config.CssProperties;
import com.agentportal.domain.*;
import com.agentportal.dto.*;
import com.agentportal.repo.*;
import com.agentportal.security.CurrentUser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SessionService {

    private final AgentSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ToolRunRepository toolRunRepository;
    private final PermissionRequestRepository permissionRepository;
    private final AgentEventRepository eventRepository;
    private final SessionCollaboratorRepository collaboratorRepository;
    private final AgentProcessManager processManager;
    private final AgentProperties agentProperties;
    private final WorkspaceFileService workspaceFileService;
    private final WorkspaceChangeService workspaceChangeService;
    private final WorkspaceQuotaService workspaceQuotaService;
    private final AuditService auditService;
    private final CssProperties cssProperties;
    private final ObjectMapper objectMapper;

    public SessionService(
            AgentSessionRepository sessionRepository,
            ChatMessageRepository messageRepository,
            ToolRunRepository toolRunRepository,
            PermissionRequestRepository permissionRepository,
            AgentEventRepository eventRepository,
            SessionCollaboratorRepository collaboratorRepository,
            AgentProcessManager processManager,
            AgentProperties agentProperties,
            WorkspaceFileService workspaceFileService,
            WorkspaceChangeService workspaceChangeService,
            WorkspaceQuotaService workspaceQuotaService,
            AuditService auditService,
            CssProperties cssProperties,
            ObjectMapper objectMapper
    ) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.toolRunRepository = toolRunRepository;
        this.permissionRepository = permissionRepository;
        this.eventRepository = eventRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.processManager = processManager;
        this.agentProperties = agentProperties;
        this.workspaceFileService = workspaceFileService;
        this.workspaceChangeService = workspaceChangeService;
        this.workspaceQuotaService = workspaceQuotaService;
        this.auditService = auditService;
        this.cssProperties = cssProperties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SessionDto create(CreateSessionRequest request) throws Exception {
        Path workspace = resolveWorkspace(request.workspacePath());
        workspaceQuotaService.assertWithinQuota(workspace);
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
        workspaceChangeService.captureBaseline(session.getId(), workspace);
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
            Set<UUID> collabIds = collaboratorRepository.findByUsername(user).stream()
                    .map(SessionCollaborator::getSessionId)
                    .collect(Collectors.toSet());
            return sessionRepository.findByStatusNotOrderByUpdatedAtDesc(SessionStatus.ARCHIVED).stream()
                    .filter(s -> user.equals(s.getOwnerUsername()) || collabIds.contains(s.getId()))
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

    public List<Map<String, Object>> events(UUID id) {
        require(id);
        return eventRepository.findBySessionIdOrderByCreatedAtAsc(id).stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId().toString());
            m.put("type", e.getType());
            m.put("createdAt", e.getCreatedAt().toString());
            try {
                m.put("payload", e.getPayloadJson() == null || e.getPayloadJson().isBlank()
                        ? Map.of()
                        : objectMapper.readValue(e.getPayloadJson(), new TypeReference<Map<String, Object>>() {}));
            } catch (Exception ex) {
                m.put("payload", Map.of("raw", e.getPayloadJson()));
            }
            return m;
        }).toList();
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

        Path workspace = Path.of(session.getWorkspacePath());
        workspaceQuotaService.assertWithinQuota(workspace);
        workspaceChangeService.captureBaseline(id, workspace);

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

    public List<FileChangeDto> listChanges(UUID sessionId) throws Exception {
        AgentSession session = require(sessionId);
        return workspaceChangeService.listChanges(sessionId, Path.of(session.getWorkspacePath()));
    }

    public FileChangeDto diffFile(UUID sessionId, String path) throws Exception {
        AgentSession session = require(sessionId);
        return workspaceChangeService.diffFile(sessionId, Path.of(session.getWorkspacePath()), path);
    }

    public Map<String, Object> acceptChange(UUID sessionId, String path) throws Exception {
        AgentSession session = require(sessionId);
        Map<String, Object> result = workspaceChangeService.accept(sessionId, Path.of(session.getWorkspacePath()), path);
        auditService.record("change.accept", sessionId.toString(), path);
        return result;
    }

    public Map<String, Object> rejectChange(UUID sessionId, String path) throws Exception {
        AgentSession session = require(sessionId);
        Map<String, Object> result = workspaceChangeService.reject(sessionId, Path.of(session.getWorkspacePath()), path);
        auditService.record("change.reject", sessionId.toString(), path);
        return result;
    }

    public List<Map<String, Object>> listCollaborators(UUID sessionId) {
        requireOwnerOnly(sessionId);
        return collaboratorRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(c -> Map.<String, Object>of(
                        "username", c.getUsername(),
                        "role", c.getRole(),
                        "createdAt", c.getCreatedAt().toString()
                ))
                .toList();
    }

    @Transactional
    public Map<String, Object> addCollaborator(UUID sessionId, String username) {
        AgentSession session = requireOwnerOnly(sessionId);
        String user = username == null ? "" : username.trim();
        if (user.isBlank()) {
            throw new IllegalArgumentException("username required");
        }
        if (user.equals(session.getOwnerUsername())) {
            throw new IllegalArgumentException("Owner is already the session owner");
        }
        SessionCollaborator existing = collaboratorRepository.findBySessionIdAndUsername(sessionId, user).orElse(null);
        if (existing == null) {
            SessionCollaborator c = new SessionCollaborator();
            c.setSessionId(sessionId);
            c.setUsername(user);
            c.setRole("collaborator");
            collaboratorRepository.save(c);
        }
        auditService.record("session.share", sessionId.toString(), user);
        return Map.of("status", "ok", "username", user);
    }

    @Transactional
    public void removeCollaborator(UUID sessionId, String username) {
        requireOwnerOnly(sessionId);
        collaboratorRepository.deleteBySessionIdAndUsername(sessionId, username);
        auditService.record("session.unshare", sessionId.toString(), username);
    }

    public void resolvePermission(UUID sessionId, UUID permissionId, PermissionDecisionRequest request) throws Exception {
        AgentSession session = require(sessionId);
        SessionAgentRuntime runtime = processManager.getOrStart(session);
        if ("antigravity".equalsIgnoreCase(session.getProvider()) && !(runtime instanceof CursorSessionRuntime)) {
            throw new IllegalStateException(
                    "Antigravity print-mode does not support mid-turn permission prompts.");
        }
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
        assertAccess(session);
        return session;
    }

    private AgentSession requireOwnerOnly(UUID id) {
        AgentSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Session not found: " + id));
        if (!enforceOwnership()) {
            return session;
        }
        if (CurrentUser.isAdmin()) {
            return session;
        }
        String user = CurrentUser.usernameOrAnonymous();
        if (!user.equals(session.getOwnerUsername())) {
            throw new NoSuchElementException("Session not found: " + id);
        }
        return session;
    }

    private void assertAccess(AgentSession session) {
        if (!enforceOwnership()) {
            return;
        }
        if (CurrentUser.isAdmin()) {
            return;
        }
        String user = CurrentUser.usernameOrAnonymous();
        String owner = session.getOwnerUsername();
        if (owner != null && owner.equals(user)) {
            return;
        }
        if (collaboratorRepository.findBySessionIdAndUsername(session.getId(), user).isPresent()) {
            return;
        }
        throw new NoSuchElementException("Session not found: " + session.getId());
    }

    private boolean enforceOwnership() {
        return cssProperties.isEnabled();
    }

    private int nextSequence(UUID sessionId) {
        return (int) (messageRepository.maxSequence(sessionId) + 1);
    }

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
