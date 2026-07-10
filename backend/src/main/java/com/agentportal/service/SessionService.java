package com.agentportal.service;

import com.agentportal.acp.AgentProcessManager;
import com.agentportal.acp.SessionAgentRuntime;
import com.agentportal.config.AgentProperties;
import com.agentportal.domain.*;
import com.agentportal.dto.*;
import com.agentportal.repo.*;
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

    public SessionService(
            AgentSessionRepository sessionRepository,
            ChatMessageRepository messageRepository,
            ToolRunRepository toolRunRepository,
            PermissionRequestRepository permissionRepository,
            AgentEventRepository eventRepository,
            AgentProcessManager processManager,
            AgentProperties agentProperties,
            WorkspaceFileService workspaceFileService
    ) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.toolRunRepository = toolRunRepository;
        this.permissionRepository = permissionRepository;
        this.processManager = processManager;
        this.agentProperties = agentProperties;
        this.workspaceFileService = workspaceFileService;
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
        session = sessionRepository.save(session);
        return SessionDto.from(session);
    }

    public List<SessionDto> list() {
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
        if (session.getStatus() == SessionStatus.STREAMING
                || session.getStatus() == SessionStatus.WAITING_PERMISSION
                || session.getStatus() == SessionStatus.WAITING_PLAN) {
            throw new IllegalStateException("Session already has an active run");
        }

        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(id);
        userMsg.setRole(MessageRole.USER);
        userMsg.setContent(request.prompt());
        userMsg.setSequenceNo(messageRepository.maxSequence(id) + 1);
        userMsg = messageRepository.save(userMsg);

        if (session.getTitle().startsWith("Session ") && !request.prompt().isEmpty()) {
            String shortTitle = request.prompt().length() > 48
                    ? request.prompt().substring(0, 48) + "…"
                    : request.prompt();
            session.setTitle(shortTitle);
            sessionRepository.save(session);
        }

        SessionAgentRuntime runtime = processManager.getOrStart(session);
        runtime.prompt(request.prompt());
        return MessageDto.from(userMsg);
    }

    public void cancel(UUID id) {
        AgentSession session = require(id);
        SessionAgentRuntime runtime = processManager.get(session.getId());
        if (runtime != null) {
            runtime.cancel();
        } else {
            session.setStatus(SessionStatus.CANCELLED);
            sessionRepository.save(session);
        }
    }

    /**
     * Abandon a nested tool/sub-agent. Marks DB row abandoned; if provider cannot
     * cancel the child alone, falls back to cancelling the whole session run.
     */
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
        return SessionDto.from(sessionRepository.save(session));
    }

    private AgentSession require(UUID id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Session not found: " + id));
    }

    private Path resolveWorkspace(String requested) {
        Path root = Path.of(agentProperties.getWorkspace().getRoot()).toAbsolutePath().normalize();
        Path path = Path.of(requested).toAbsolutePath().normalize();
        if (!path.startsWith(root) && !requested.contains(":") && !requested.startsWith("/") && !requested.startsWith("\\")) {
            path = root.resolve(requested).normalize();
        }
        return path;
    }
}
