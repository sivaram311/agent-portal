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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
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
    private final TransactionTemplate transactionTemplate;
    private final GuidanceService guidanceService;
    private final RoleAclService roleAclService;

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
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate,
            GuidanceService guidanceService,
            RoleAclService roleAclService
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
        this.transactionTemplate = transactionTemplate;
        this.guidanceService = guidanceService;
        this.roleAclService = roleAclService;
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
        if (request.platformRole() != null && !request.platformRole().isBlank()) {
            session.setPlatformRole(roleAclService.normalizeRole(request.platformRole()));
        }
        if (request.platformTaskId() != null) {
            session.setPlatformTaskId(request.platformTaskId());
        }
        session = sessionRepository.save(session);
        boolean useDefaults = request.useGuidanceDefaults() == null || request.useGuidanceDefaults();
        guidanceService.seedDefaultsForNewSession(session.getId(), useDefaults);
        try {
            guidanceService.materializeForSession(session.getWorkspacePath(), session.getId());
        } catch (Exception e) {
            // Non-fatal at create — prompt path will retry
        }
        workspaceChangeService.captureBaseline(session.getId(), workspace);
        auditService.record("session.create", session.getId().toString(), provider + " @ " + workspace);
        return toDto(session);
    }

    public List<SessionDto> list() {
        if (enforceOwnership()) {
            String user = CurrentUser.usernameOrAnonymous();
            if (CurrentUser.isAdmin()) {
                return sessionRepository.findAllByOrderByUpdatedAtDesc()
                        .stream()
                        .map(this::toDto)
                        .toList();
            }
            Set<UUID> collabIds = collaboratorRepository.findByUsername(user).stream()
                    .map(SessionCollaborator::getSessionId)
                    .collect(Collectors.toSet());
            return sessionRepository.findAllByOrderByUpdatedAtDesc().stream()
                    .filter(s -> user.equals(s.getOwnerUsername()) || collabIds.contains(s.getId()))
                    .map(this::toDto)
                    .toList();
        }
        return sessionRepository.findAllByOrderByUpdatedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public SessionDto get(UUID id) {
        return toDto(require(id));
    }

    @Transactional
    public SessionDto updatePlatformRole(UUID id, UpdateSessionRoleRequest request) {
        AgentSession session = require(id);
        if (request.platformRole() != null) {
            if (request.platformRole().isBlank()) {
                session.setPlatformRole(null);
            } else {
                session.setPlatformRole(roleAclService.normalizeRole(request.platformRole()));
            }
        }
        if (request.platformTaskId() != null) {
            session.setPlatformTaskId(request.platformTaskId());
        }
        auditService.record("session.platform_role", id.toString(),
                String.valueOf(session.getPlatformRole()));
        return toDto(sessionRepository.save(session));
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

    /**
     * Persist the user message, then start/prompt the agent outside any long DB transaction.
     * ACP handshake can take tens of seconds; holding a transaction open causes pool stalls and opaque 500s.
     */
    public MessageDto prompt(UUID id, PromptRequest request) throws Exception {
        ChatMessage userMsg;
        try {
            userMsg = transactionTemplate.execute(status -> {
                try {
                    return recordUserPrompt(id, request);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        AgentSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Session not found: " + id));
        guidanceService.materializeForSession(session.getWorkspacePath(), session.getId());
        String prefix = guidanceService.buildPromptPrefix(session.getId());
        String rolePrefix = roleAclService.findRole(session.getPlatformRole())
                .map(roleAclService::buildRolePromptPrefix)
                .orElse("");
        String agentPrompt = (rolePrefix + prefix).isBlank()
                ? request.prompt()
                : rolePrefix + prefix + request.prompt();
        SessionAgentRuntime runtime = processManager.getOrStart(session);
        runtime.prompt(agentPrompt);
        return MessageDto.from(userMsg);
    }

    private ChatMessage recordUserPrompt(UUID id, PromptRequest request) throws IOException {
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
        auditService.record("session.prompt", id.toString(), "len=" + request.prompt().length());
        maybeAutotitleFromPrompt(session, request.prompt());
        return userMsg;
    }

    /** Replace placeholder titles with a short prompt summary so mobile lists stay scannable. */
    private void maybeAutotitleFromPrompt(AgentSession session, String prompt) {
        if (!isPlaceholderTitle(session.getTitle()) || prompt == null || prompt.isBlank()) {
            return;
        }
        String compact = prompt.trim().replaceAll("\\s+", " ");
        if (compact.length() > 72) {
            compact = compact.substring(0, 69) + "...";
        }
        session.setTitle(compact);
        sessionRepository.save(session);
    }

    static boolean isPlaceholderTitle(String title) {
        if (title == null || title.isBlank()) {
            return true;
        }
        String t = title.trim();
        if ("New session".equalsIgnoreCase(t)) {
            return true;
        }
        return t.regionMatches(true, 0, "Session ", 0, "Session ".length())
                && t.length() > 8
                && Character.isDigit(t.charAt(8));
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
        roleAclService.assertSessionAction(session.getPlatformRole(), "listFiles");
        return workspaceFileService.list(Path.of(session.getWorkspacePath()), path);
    }

    public FileContentDto readFile(UUID sessionId, String path) throws Exception {
        AgentSession session = require(sessionId);
        roleAclService.assertSessionAction(session.getPlatformRole(), "readFile");
        return workspaceFileService.read(Path.of(session.getWorkspacePath()), path);
    }

    public List<FileChangeDto> listChanges(UUID sessionId) throws Exception {
        AgentSession session = require(sessionId);
        roleAclService.assertSessionAction(session.getPlatformRole(), "listChanges");
        return workspaceChangeService.listChanges(sessionId, Path.of(session.getWorkspacePath()));
    }

    public FileChangeDto diffFile(UUID sessionId, String path) throws Exception {
        AgentSession session = require(sessionId);
        roleAclService.assertSessionAction(session.getPlatformRole(), "diffChange");
        return workspaceChangeService.diffFile(sessionId, Path.of(session.getWorkspacePath()), path);
    }

    public Map<String, Object> acceptChange(UUID sessionId, String path) throws Exception {
        AgentSession session = require(sessionId);
        roleAclService.assertSessionAction(session.getPlatformRole(), "acceptChange");
        Map<String, Object> result = workspaceChangeService.accept(sessionId, Path.of(session.getWorkspacePath()), path);
        auditService.record("change.accept", sessionId.toString(), path);
        return result;
    }

    public Map<String, Object> rejectChange(UUID sessionId, String path) throws Exception {
        AgentSession session = require(sessionId);
        roleAclService.assertSessionAction(session.getPlatformRole(), "rejectChange");
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
        String decision = request.decision() == null ? "" : request.decision().toLowerCase(Locale.ROOT);
        boolean allowing = !(decision.contains("reject") || decision.contains("deny"));
        if (allowing && session.getPlatformRole() != null && !session.getPlatformRole().isBlank()) {
            var pending = permissionRepository.findByIdAndSessionId(permissionId, sessionId)
                    .orElseThrow(() -> new NoSuchElementException("Permission not found"));
            String category = roleAclService.classifyToolFromDetailsJson(pending.getDetailsJson());
            var role = roleAclService.requireRole(session.getPlatformRole());
            if (!roleAclService.isToolAllowed(role, category)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Role " + role.id() + " cannot allow tool category '" + category + "'");
            }
        }
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
        return toDto(sessionRepository.save(session));
    }

    @Transactional
    public SessionDto unarchive(UUID id) {
        AgentSession session = require(id);
        if (session.getStatus() != SessionStatus.ARCHIVED) {
            return toDto(session);
        }
        // Stale mid-turn permissions cannot be answered after archive stopped ACP.
        for (var pending : permissionRepository.findBySessionIdAndStatusOrderByCreatedAtDesc(
                id, PermissionStatus.PENDING)) {
            pending.setStatus(PermissionStatus.REJECT_ONCE);
            pending.setResolvedAt(java.time.Instant.now());
            permissionRepository.save(pending);
        }
        // Drop dead Cursor conversation id so the next prompt opens a fresh ACP session.
        session.setCursorSessionId(null);
        session.setStatus(SessionStatus.IDLE);
        auditService.record("session.unarchive", id.toString(), null);
        return toDto(sessionRepository.save(session));
    }

    public SessionGuidanceDto getGuidance(UUID id) {
        require(id);
        return guidanceService.getSessionGuidance(id);
    }

    @Transactional
    public SessionGuidanceDto putGuidance(UUID id, UpdateSessionGuidanceRequest request) {
        AgentSession session = requireOwnerOnly(id);
        SessionGuidanceDto dto = guidanceService.putSessionGuidance(
                id,
                request,
                new GuidanceService.PathAwareSession(session.getWorkspacePath())
        );
        auditService.record("session.guidance", id.toString(), "items=" + (request.items() == null ? 0 : request.items().size()));
        return dto;
    }

    public boolean canAccess(UUID id) {
        try {
            require(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private SessionDto toDto(AgentSession session) {
        return SessionDto.from(session, roleAclService.findRole(session.getPlatformRole()).orElse(null));
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
