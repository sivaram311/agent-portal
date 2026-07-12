package com.agentportal.service;

import com.agentportal.domain.AgentSession;
import com.agentportal.domain.PlatformAgentMessage;
import com.agentportal.domain.PlatformApp;
import com.agentportal.domain.PlatformMemoryEntry;
import com.agentportal.domain.PlatformTask;
import com.agentportal.domain.PortLease;
import com.agentportal.domain.SessionStatus;
import com.agentportal.dto.ClaimPortRequest;
import com.agentportal.dto.CreatePlatformAgentMessageRequest;
import com.agentportal.dto.CreatePlatformTaskRequest;
import com.agentportal.dto.CreateSessionRequest;
import com.agentportal.dto.E2eLoopProgressDto;
import com.agentportal.dto.LinkTaskSessionRequest;
import com.agentportal.dto.PlatformAgentMessageDto;
import com.agentportal.dto.PlatformAppDto;
import com.agentportal.dto.PlatformMemoryDto;
import com.agentportal.dto.PlatformOrgDto;
import com.agentportal.dto.PlatformPipelineDto;
import com.agentportal.dto.PlatformProjectSummaryDto;
import com.agentportal.dto.PlatformRoleDto;
import com.agentportal.dto.PlatformTaskDto;
import com.agentportal.dto.PortLeaseDto;
import com.agentportal.dto.PromptRequest;
import com.agentportal.dto.RunPlatformPipelineRequest;
import com.agentportal.dto.SessionDto;
import com.agentportal.dto.SwarmTickRequest;
import com.agentportal.dto.SwarmTickResultDto;
import com.agentportal.dto.UpdatePlatformAgentMessageRequest;
import com.agentportal.dto.UpdatePlatformTaskRequest;
import com.agentportal.dto.UpsertPlatformMemoryRequest;
import com.agentportal.repo.AgentSessionRepository;
import com.agentportal.repo.PlatformAgentMessageRepository;
import com.agentportal.repo.PlatformAppRepository;
import com.agentportal.repo.PlatformMemoryRepository;
import com.agentportal.repo.PlatformTaskRepository;
import com.agentportal.repo.PortLeaseRepository;
import com.agentportal.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class PlatformRegistryService {

    private static final Logger log = LoggerFactory.getLogger(PlatformRegistryService.class);

    private static final Set<String> ROLES = Set.of(
            "ARCHITECTURE", "BACKEND", "FRONTEND", "QA", "DEVOPS", "PRODUCT", "SECURITY");
    private static final Set<String> MESSAGE_ROLES = Set.of(
            "ARCHITECTURE", "BACKEND", "FRONTEND", "QA", "DEVOPS", "PRODUCT", "SECURITY", "EM", "*");
    private static final Set<String> STATUSES = Set.of(
            "OPEN", "ASSIGNED", "IN_PROGRESS", "BLOCKED", "DONE", "CANCELLED");
    private static final Set<String> MEMORY_KINDS = Set.of(
            "NOTE", "DECISION", "CONTRACT", "ARTIFACT", "MESSAGE_SUMMARY", "PROGRESS");
    private static final String PIPELINE_SYSTEM_E2E = "SYSTEM_E2E_LOOP";
    private static final int DEFAULT_E2E_MAX_ITERATIONS = 20;
    private static final Set<String> OUTCOMES = Set.of("PASS", "FAIL", "FLAKY");
    private static final Set<String> STEP_KEYS = Set.of("RUN", "QA", "FIX", "DOCS", "REVIEW");
    private static final List<String> TOOLS_READ = List.of("read", "search", "docs");
    private static final List<String> TOOLS_IMPL = List.of("read", "search", "edit", "shell", "docs");
    private static final List<String> ACTIONS_IMPL = List.of(
            "listFiles", "readFile", "listChanges", "diffChange", "acceptChange", "rejectChange", "sendPrompt");
    private static final List<String> ACTIONS_REVIEW = List.of(
            "listChanges", "diffChange", "acceptChange", "rejectChange", "listAudit");
    private static final List<PlatformRoleDto> ROLE_DTOS = List.of(
            new PlatformRoleDto("ARCHITECTURE", "Architecture", "Engineering",
                    "sandbox/platform-tasks/architecture", "ap-platform-em",
                    TOOLS_READ, List.of("listFiles", "readFile", "sendPrompt", "listMemory", "upsertMemory"),
                    "Design the approach; write CONTRACT memory; do not implement production code.",
                    true),
            new PlatformRoleDto("BACKEND", "Backend", "Engineering",
                    "sandbox/platform-tasks/backend", "ap-platform-em",
                    TOOLS_IMPL, ACTIONS_IMPL,
                    "Implement APIs/data under sandbox; publish CONTRACT updates; message FRONTEND when ready.",
                    false),
            new PlatformRoleDto("FRONTEND", "Frontend", "Engineering",
                    "sandbox/platform-tasks/frontend", "ap-platform-em",
                    List.of("read", "search", "edit", "docs", "browser"), ACTIONS_IMPL,
                    "Implement UI against CONTRACT memory; keep Changes reviewable.",
                    false),
            new PlatformRoleDto("QA", "QA", "Engineering",
                    "sandbox/platform-tasks/qa", "ap-platform-qa",
                    List.of("read", "search", "shell", "browser", "test"),
                    List.of("listFiles", "readFile", "sendPrompt", "listChanges", "listAudit"),
                    "Verify acceptance criteria; block promote on severity ≥ threshold.",
                    true),
            new PlatformRoleDto("DEVOPS", "DevOps", "Engineering",
                    "sandbox/platform-tasks/devops", "ap-platform-ops",
                    List.of("read", "shell", "ports", "deploy", "docs"),
                    List.of("listPorts", "claimPort", "releasePort", "listApps", "sendPrompt"),
                    "Claim ports; propose staging/prod via VERSIONING-PROMOTE; never mass-kill by process name.",
                    true),
            new PlatformRoleDto("PRODUCT", "Product", "Product",
                    "sandbox/platform-tasks/product", "ap-platform-em",
                    TOOLS_READ, List.of("listFiles", "readFile", "sendPrompt", "listMemory", "upsertMemory"),
                    "Clarify goals and acceptance criteria; write DECISION memory.",
                    true),
            new PlatformRoleDto("SECURITY", "Security", "Security",
                    "sandbox/platform-tasks/security", "ap-platform-review",
                    List.of("read", "search", "audit", "docs"), ACTIONS_REVIEW,
                    "Audit for secrets, sandbox escapes, and protocol violations; require human merge approval.",
                    true)
    );
    private static final Map<String, PlatformPipelineDto> PIPELINES = new LinkedHashMap<>();

    static {
        PIPELINES.put("FEATURE", PlatformPipelineDto.standard(
                "FEATURE",
                "Feature delivery",
                "Clarify → design → implement → verify → deploy proposal",
                List.of("PRODUCT", "ARCHITECTURE", "BACKEND", "FRONTEND", "QA", "DEVOPS")));
        PIPELINES.put("BUGFIX", PlatformPipelineDto.standard(
                "BUGFIX",
                "Bug fix",
                "Reproduce → fix → verify",
                List.of("BACKEND", "FRONTEND", "QA")));
        PIPELINES.put("REFACTOR", PlatformPipelineDto.standard(
                "REFACTOR",
                "Refactor",
                "Design → change → regression QA",
                List.of("ARCHITECTURE", "BACKEND", "FRONTEND", "QA")));
        PIPELINES.put("SECURITY_AUDIT", PlatformPipelineDto.standard(
                "SECURITY_AUDIT",
                "Security audit",
                "Audit → harden → retest",
                List.of("SECURITY", "BACKEND", "QA")));
        PIPELINES.put(PIPELINE_SYSTEM_E2E, PlatformPipelineDto.systemLoop(
                PIPELINE_SYSTEM_E2E,
                "System E2E loop",
                "Playwright QA → fix → docs → human review/commit → retest until green (default max 20)",
                List.of("QA", "BACKEND", "PRODUCT", "SECURITY"),
                DEFAULT_E2E_MAX_ITERATIONS));
    }

    private final PortLeaseRepository portLeaseRepository;
    private final PlatformAppRepository platformAppRepository;
    private final PlatformTaskRepository platformTaskRepository;
    private final PlatformMemoryRepository platformMemoryRepository;
    private final PlatformAgentMessageRepository platformAgentMessageRepository;
    private final AgentSessionRepository agentSessionRepository;
    private final WorkspacePathResolver workspacePathResolver;
    private final SessionService sessionService;

    public PlatformRegistryService(
            PortLeaseRepository portLeaseRepository,
            PlatformAppRepository platformAppRepository,
            PlatformTaskRepository platformTaskRepository,
            PlatformMemoryRepository platformMemoryRepository,
            PlatformAgentMessageRepository platformAgentMessageRepository,
            AgentSessionRepository agentSessionRepository,
            WorkspacePathResolver workspacePathResolver,
            @Lazy SessionService sessionService
    ) {
        this.portLeaseRepository = portLeaseRepository;
        this.platformAppRepository = platformAppRepository;
        this.platformTaskRepository = platformTaskRepository;
        this.platformMemoryRepository = platformMemoryRepository;
        this.platformAgentMessageRepository = platformAgentMessageRepository;
        this.agentSessionRepository = agentSessionRepository;
        this.workspacePathResolver = workspacePathResolver;
        this.sessionService = sessionService;
    }

    @Transactional(readOnly = true)
    public List<PortLeaseDto> listPorts(boolean activeOnly) {
        List<PortLease> rows = activeOnly
                ? portLeaseRepository.findByStatusOrderByPortAsc("active")
                : portLeaseRepository.findAllByOrderByPortAsc();
        return rows.stream().map(PortLeaseDto::from).toList();
    }

    @Transactional
    public PortLeaseDto claim(ClaimPortRequest req) {
        String user = CurrentUser.username().orElse("anonymous");
        PortLease existing = portLeaseRepository.findByPort(req.port()).orElse(null);
        if (existing != null && "active".equalsIgnoreCase(existing.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Port " + req.port() + " already claimed by " + existing.getOwnerApp());
        }
        PortLease lease = existing != null ? existing : new PortLease();
        lease.setPort(req.port());
        lease.setService(req.service().trim());
        lease.setOwnerApp(req.ownerApp().trim());
        lease.setEnv(req.env() == null || req.env().isBlank() ? "host" : req.env().trim());
        lease.setBindAddress(req.bindAddress() == null || req.bindAddress().isBlank()
                ? "0.0.0.0" : req.bindAddress().trim());
        lease.setStatus("active");
        lease.setClaimedBy(user);
        lease.setClaimedAt(Instant.now());
        lease.setReleasedAt(null);
        lease.setNotes(req.notes());
        return PortLeaseDto.from(portLeaseRepository.save(lease));
    }

    @Transactional
    public PortLeaseDto release(int port) {
        PortLease lease = portLeaseRepository.findByPort(port)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Port not found"));
        lease.setStatus("released");
        lease.setReleasedAt(Instant.now());
        return PortLeaseDto.from(portLeaseRepository.save(lease));
    }

    @Transactional(readOnly = true)
    public List<PlatformAppDto> listApps(boolean enabledOnly) {
        List<PlatformApp> rows = enabledOnly
                ? platformAppRepository.findByEnabledTrueOrderByNameAsc()
                : platformAppRepository.findAllByOrderByNameAsc();
        return rows.stream().map(PlatformAppDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PlatformTaskDto> listTasks(String status, String role) {
        String normalizedStatus = normalizeOptionalStatus(status);
        String normalizedRole = normalizeOptionalRole(role);
        List<PlatformTask> rows;
        if (normalizedStatus != null && normalizedRole != null) {
            rows = platformTaskRepository.findByStatusAndRoleOrderByCreatedAtDesc(normalizedStatus, normalizedRole);
        } else if (normalizedStatus != null) {
            rows = platformTaskRepository.findByStatusOrderByCreatedAtDesc(normalizedStatus);
        } else if (normalizedRole != null) {
            rows = platformTaskRepository.findByRoleOrderByCreatedAtDesc(normalizedRole);
        } else {
            rows = platformTaskRepository.findAllByOrderByCreatedAtDesc();
        }
        return rows.stream().map(PlatformTaskDto::from).toList();
    }

    @Transactional(readOnly = true)
    public PlatformTaskDto getTask(UUID id) {
        return PlatformTaskDto.from(findTask(id));
    }

    @Transactional
    public PlatformTaskDto createTask(CreatePlatformTaskRequest req) {
        PlatformTask task = new PlatformTask();
        task.setTitle(requireText(req.title(), "title"));
        task.setDescription(req.description());
        task.setRole(normalizeRole(req.role()));
        task.setStatus(req.status() == null || req.status().isBlank() ? "OPEN" : normalizeStatus(req.status()));
        task.setProjectSlug(trimToNull(req.projectSlug()));
        task.setCreatedBy(CurrentUser.usernameOrAnonymous());
        task.setAssigneeUsername(trimToNull(req.assigneeUsername()));
        if (req.sessionId() != null) {
            requireSession(req.sessionId());
            task.setSessionId(req.sessionId());
        }
        if (req.parentTaskId() != null) {
            findTask(req.parentTaskId());
            task.setParentTaskId(req.parentTaskId());
        }
        task.setPipelineId(trimToNull(req.pipelineId()));
        if (req.iteration() != null) {
            task.setIteration(req.iteration());
        }
        if (req.maxIterations() != null) {
            task.setMaxIterations(req.maxIterations());
        }
        if (req.outcome() != null) {
            task.setOutcome(normalizeOutcome(req.outcome()));
        }
        if (req.stepKey() != null) {
            task.setStepKey(normalizeStepKey(req.stepKey()));
        }
        PlatformTask saved = platformTaskRepository.save(task);
        saved.setWorkspacePath(resolveWorkspacePath(req.workspacePath(), saved.getId()));
        return PlatformTaskDto.from(platformTaskRepository.save(saved));
    }

    @Transactional
    public PlatformTaskDto updateTask(UUID id, UpdatePlatformTaskRequest req) {
        PlatformTask task = findTask(id);
        String previousStatus = task.getStatus();
        if (req.title() != null) {
            task.setTitle(requireText(req.title(), "title"));
        }
        if (req.description() != null) {
            task.setDescription(req.description());
        }
        if (req.role() != null) {
            task.setRole(normalizeRole(req.role()));
        }
        if (req.status() != null) {
            task.setStatus(normalizeStatus(req.status()));
        }
        if (req.projectSlug() != null) {
            task.setProjectSlug(trimToNull(req.projectSlug()));
        }
        if (req.workspacePath() != null) {
            task.setWorkspacePath(resolveWorkspacePath(req.workspacePath(), id));
        }
        if (req.assigneeUsername() != null) {
            task.setAssigneeUsername(trimToNull(req.assigneeUsername()));
        }
        if (req.sessionId() != null) {
            requireSession(req.sessionId());
            task.setSessionId(req.sessionId());
        }
        if (req.parentTaskId() != null) {
            if (req.parentTaskId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "parentTaskId cannot equal task id");
            }
            findTask(req.parentTaskId());
            task.setParentTaskId(req.parentTaskId());
        }
        if (req.pipelineId() != null) {
            task.setPipelineId(trimToNull(req.pipelineId()));
        }
        if (req.iteration() != null) {
            task.setIteration(req.iteration());
        }
        if (req.maxIterations() != null) {
            task.setMaxIterations(req.maxIterations());
        }
        if (req.outcome() != null) {
            task.setOutcome(normalizeOutcome(req.outcome()));
        }
        if (req.stepKey() != null) {
            task.setStepKey(normalizeStepKey(req.stepKey()));
        }
        PlatformTask saved = platformTaskRepository.save(task);
        if (!"DONE".equals(previousStatus) && "DONE".equals(saved.getStatus())) {
            advancePipelineAfterDone(saved);
        }
        return PlatformTaskDto.from(saved);
    }

    @Transactional
    public PlatformTaskDto linkTaskSession(UUID id, LinkTaskSessionRequest req) {
        PlatformTask task = findTask(id);
        requireSession(req.sessionId());
        task.setSessionId(req.sessionId());
        if ("OPEN".equals(task.getStatus())) {
            task.setStatus("ASSIGNED");
        }
        AgentSession session = agentSessionRepository.findById(req.sessionId()).orElseThrow();
        session.setPlatformRole(task.getRole());
        session.setPlatformTaskId(task.getId());
        agentSessionRepository.save(session);
        return PlatformTaskDto.from(platformTaskRepository.save(task));
    }

    /**
     * Create (or reuse) a portal agent session for a platform task and send the handoff prompt —
     * same path as manual "New session" + prompt.
     */
    public SessionDto invokeTaskSession(UUID taskId) {
        PlatformTask task = findTask(taskId);
        if (task.getSessionId() != null) {
            AgentSession existing = agentSessionRepository.findById(task.getSessionId()).orElse(null);
            if (existing != null && existing.getStatus() != SessionStatus.ARCHIVED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Task already linked to session " + existing.getId()
                                + " — open that session and prompt, or clear sessionId first");
            }
        }
        String handoff = task.getDescription() == null || task.getDescription().isBlank()
                ? "Begin " + task.getRole() + " work for task " + task.getTitle()
                : task.getDescription();
        try {
            return invokeStepSessionNow(task, handoff);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Failed to invoke agent session: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<PlatformRoleDto> listRoles() {
        return ROLE_DTOS;
    }

    @Transactional(readOnly = true)
    public PlatformRoleDto getRole(String roleId) {
        String normalized = normalizeRole(roleId);
        return ROLE_DTOS.stream()
                .filter(r -> r.id().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
    }

    @Transactional(readOnly = true)
    public PlatformOrgDto orgDashboard() {
        List<PlatformTask> tasks = platformTaskRepository.findAllByOrderByCreatedAtDesc();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        Map<String, Long> byRole = new LinkedHashMap<>();
        for (String status : List.of("OPEN", "ASSIGNED", "IN_PROGRESS", "BLOCKED", "DONE", "CANCELLED")) {
            byStatus.put(status, 0L);
        }
        for (PlatformRoleDto role : ROLE_DTOS) {
            byRole.put(role.id(), 0L);
        }
        long linked = 0;
        Map<String, List<PlatformTask>> byProject = new LinkedHashMap<>();
        for (PlatformTask task : tasks) {
            byStatus.merge(task.getStatus(), 1L, Long::sum);
            byRole.merge(task.getRole(), 1L, Long::sum);
            if (task.getSessionId() != null) {
                linked++;
            }
            if (task.getProjectSlug() != null && !task.getProjectSlug().isBlank()) {
                byProject.computeIfAbsent(task.getProjectSlug(), k -> new ArrayList<>()).add(task);
            }
        }
        List<PlatformProjectSummaryDto> projects = byProject.entrySet().stream()
                .map(e -> {
                    List<PlatformTask> rows = e.getValue();
                    long open = rows.stream().filter(t -> Set.of("OPEN", "ASSIGNED", "IN_PROGRESS").contains(t.getStatus())).count();
                    long done = rows.stream().filter(t -> "DONE".equals(t.getStatus())).count();
                    long blocked = rows.stream().filter(t -> "BLOCKED".equals(t.getStatus())).count();
                    long linkedSessions = rows.stream().filter(t -> t.getSessionId() != null).count();
                    String pipelineId = rows.stream()
                            .map(PlatformTask::getPipelineId)
                            .filter(p -> p != null && !p.isBlank())
                            .findFirst()
                            .orElse(null);
                    return new PlatformProjectSummaryDto(
                            e.getKey(), rows.size(), open, done, blocked, linkedSessions, pipelineId);
                })
                .sorted((a, b) -> Long.compare(b.openCount(), a.openCount()))
                .toList();
        List<PlatformTaskDto> blocked = tasks.stream()
                .filter(t -> "BLOCKED".equals(t.getStatus()))
                .limit(20)
                .map(PlatformTaskDto::from)
                .toList();
        List<PlatformTaskDto> recentOpen = tasks.stream()
                .filter(t -> Set.of("OPEN", "ASSIGNED", "IN_PROGRESS").contains(t.getStatus()))
                .limit(20)
                .map(PlatformTaskDto::from)
                .toList();
        long unread = platformAgentMessageRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(m -> "UNREAD".equals(m.getStatus()))
                .count();
        long memory = platformMemoryRepository.count();
        return new PlatformOrgDto(
                "VirtualDev Co",
                byStatus,
                byRole,
                unread,
                memory,
                linked,
                projects.size(),
                projects,
                blocked,
                recentOpen,
                ROLE_DTOS
        );
    }

    @Transactional
    public SwarmTickResultDto swarmTick(SwarmTickRequest req) {
        String projectSlug = trimToNull(req == null ? null : req.projectSlug());
        List<PlatformTask> candidates = projectSlug == null
                ? platformTaskRepository.findAllByOrderByCreatedAtDesc()
                : platformTaskRepository.findByProjectSlugOrderByCreatedAtDesc(projectSlug);
        List<SwarmTickResultDto.SwarmActionDto> actions = new ArrayList<>();
        int advanced = 0;
        int parentsCompleted = 0;
        int messagesSent = 0;

        // Re-run advance for any DONE children that still have OPEN next siblings.
        for (PlatformTask task : candidates) {
            if (!"DONE".equals(task.getStatus()) || task.getParentTaskId() == null) {
                continue;
            }
            SwarmAdvanceResult result = advancePipelineAfterDone(task);
            advanced += result.advanced();
            parentsCompleted += result.parentsCompleted();
            messagesSent += result.messagesSent();
            actions.addAll(result.actions());
        }

        // Activate the first OPEN child under each IN_PROGRESS pipeline parent.
        Map<UUID, List<PlatformTask>> childrenByParent = new LinkedHashMap<>();
        for (PlatformTask task : candidates) {
            if (task.getParentTaskId() != null) {
                childrenByParent.computeIfAbsent(task.getParentTaskId(), k -> new ArrayList<>()).add(task);
            }
        }
        for (Map.Entry<UUID, List<PlatformTask>> entry : childrenByParent.entrySet()) {
            PlatformTask parent = platformTaskRepository.findById(entry.getKey()).orElse(null);
            if (parent == null || !"IN_PROGRESS".equals(parent.getStatus())) {
                continue;
            }
            List<PlatformTask> children = entry.getValue().stream()
                    .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                    .toList();
            boolean hasActive = children.stream()
                    .anyMatch(c -> Set.of("ASSIGNED", "IN_PROGRESS").contains(c.getStatus()));
            if (hasActive) {
                continue;
            }
            PlatformTask next = children.stream()
                    .filter(c -> "OPEN".equals(c.getStatus()))
                    .findFirst()
                    .orElse(null);
            if (next == null) {
                continue;
            }
            next.setStatus("ASSIGNED");
            platformTaskRepository.save(next);
            String activateBody = "Pipeline ready — begin " + next.getRole() + " work.";
            sendSwarmHandoff(next, activateBody);
            if (PIPELINE_SYSTEM_E2E.equalsIgnoreCase(
                    next.getPipelineId() == null ? "" : next.getPipelineId())) {
                scheduleStepSessionInvoke(next, activateBody);
            }
            advanced++;
            messagesSent++;
            actions.add(new SwarmTickResultDto.SwarmActionDto(
                    "ACTIVATE", next.getId(), next.getRole(), "Assigned next OPEN pipeline step"));
        }

        return new SwarmTickResultDto(projectSlug, advanced, parentsCompleted, messagesSent, actions);
    }

    @Transactional(readOnly = true)
    public List<PlatformMemoryDto> listMemory(String projectSlug, String kind) {
        String normalizedKind = normalizeOptionalMemoryKind(kind);
        List<PlatformMemoryEntry> rows;
        if (projectSlug != null && !projectSlug.isBlank() && normalizedKind != null) {
            rows = platformMemoryRepository.findByProjectSlugAndKindOrderByUpdatedAtDesc(
                    projectSlug.trim(), normalizedKind);
        } else if (projectSlug != null && !projectSlug.isBlank()) {
            rows = platformMemoryRepository.findByProjectSlugOrderByUpdatedAtDesc(projectSlug.trim());
        } else {
            rows = platformMemoryRepository.findAllByOrderByUpdatedAtDesc();
        }
        return rows.stream().map(PlatformMemoryDto::from).toList();
    }

    @Transactional
    public PlatformMemoryDto upsertMemory(UpsertPlatformMemoryRequest req) {
        String projectSlug = requireText(req.projectSlug(), "projectSlug");
        String key = requireText(req.key(), "key");
        String kind = req.kind() == null || req.kind().isBlank() ? "NOTE" : normalizeMemoryKind(req.kind());
        String value = requireText(req.value(), "value");
        PlatformMemoryEntry entry = platformMemoryRepository
                .findByProjectSlugAndEntryKey(projectSlug, key)
                .orElseGet(PlatformMemoryEntry::new);
        entry.setProjectSlug(projectSlug);
        entry.setEntryKey(key);
        entry.setKind(kind);
        entry.setValue(value);
        if (entry.getCreatedBy() == null) {
            entry.setCreatedBy(CurrentUser.usernameOrAnonymous());
        }
        return PlatformMemoryDto.from(platformMemoryRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public PlatformMemoryDto getMemory(UUID id) {
        return PlatformMemoryDto.from(platformMemoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memory entry not found")));
    }

    @Transactional
    public void deleteMemory(UUID id) {
        if (!platformMemoryRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Memory entry not found");
        }
        platformMemoryRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<PlatformAgentMessageDto> listMessages(String projectSlug, String toRole, String status) {
        String normalizedTo = normalizeOptionalMessageRole(toRole);
        String normalizedStatus = status == null || status.isBlank()
                ? null
                : requireText(status, "status").toUpperCase(Locale.ROOT);
        List<PlatformAgentMessage> rows;
        if (projectSlug != null && !projectSlug.isBlank() && normalizedTo != null) {
            rows = platformAgentMessageRepository.findByProjectSlugAndToRoleOrderByCreatedAtDesc(
                    projectSlug.trim(), normalizedTo);
        } else if (projectSlug != null && !projectSlug.isBlank()) {
            rows = platformAgentMessageRepository.findByProjectSlugOrderByCreatedAtDesc(projectSlug.trim());
        } else if (normalizedTo != null && normalizedStatus != null) {
            rows = platformAgentMessageRepository.findByToRoleAndStatusOrderByCreatedAtDesc(
                    normalizedTo, normalizedStatus);
        } else {
            rows = platformAgentMessageRepository.findAllByOrderByCreatedAtDesc();
        }
        if (normalizedStatus != null && (projectSlug != null && !projectSlug.isBlank() || normalizedTo == null)) {
            rows = rows.stream().filter(m -> normalizedStatus.equals(m.getStatus())).toList();
        }
        return rows.stream().map(PlatformAgentMessageDto::from).toList();
    }

    @Transactional
    public PlatformAgentMessageDto createMessage(CreatePlatformAgentMessageRequest req) {
        if (req.taskId() != null) {
            findTask(req.taskId());
        }
        PlatformAgentMessage msg = new PlatformAgentMessage();
        msg.setProjectSlug(requireText(req.projectSlug(), "projectSlug"));
        msg.setTaskId(req.taskId());
        msg.setFromRole(normalizeMessageRole(req.fromRole()));
        msg.setToRole(normalizeMessageRole(req.toRole()));
        msg.setSubject(requireText(req.subject(), "subject"));
        msg.setBody(requireText(req.body(), "body"));
        msg.setStatus("UNREAD");
        msg.setCreatedBy(CurrentUser.usernameOrAnonymous());
        return PlatformAgentMessageDto.from(platformAgentMessageRepository.save(msg));
    }

    @Transactional
    public PlatformAgentMessageDto updateMessage(UUID id, UpdatePlatformAgentMessageRequest req) {
        PlatformAgentMessage msg = platformAgentMessageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        if (req.status() != null) {
            String status = requireText(req.status(), "status").toUpperCase(Locale.ROOT);
            if (!Set.of("UNREAD", "READ").contains(status)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid message status");
            }
            msg.setStatus(status);
        }
        return PlatformAgentMessageDto.from(platformAgentMessageRepository.save(msg));
    }

    @Transactional(readOnly = true)
    public List<PlatformPipelineDto> listPipelines() {
        return List.copyOf(PIPELINES.values());
    }

    @Transactional
    public List<PlatformTaskDto> runPipeline(String pipelineId, RunPlatformPipelineRequest req) {
        PlatformPipelineDto pipeline = PIPELINES.get(requireText(pipelineId, "pipelineId").toUpperCase(Locale.ROOT));
        if (pipeline == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown pipeline: " + pipelineId);
        }
        String title = requireText(req.title(), "title");
        String projectSlug = requireText(req.projectSlug(), "projectSlug");
        String description = req.description();
        String user = CurrentUser.usernameOrAnonymous();

        PlatformTask parent = new PlatformTask();
        parent.setTitle(title);
        parent.setDescription(description == null || description.isBlank()
                ? pipeline.description()
                : description);
        parent.setRole("PRODUCT");
        parent.setStatus("IN_PROGRESS");
        parent.setProjectSlug(projectSlug);
        parent.setCreatedBy(user);
        parent.setPipelineId(pipeline.id());
        parent.setStepKey("RUN");

        List<PlatformTaskDto> created = new ArrayList<>();
        if (pipeline.looping()) {
            int maxIterations = resolveMaxIterations(pipeline, req.maxIterations());
            parent.setIteration(1);
            parent.setMaxIterations(maxIterations);
            PlatformTask savedParent = platformTaskRepository.save(parent);
            savedParent.setWorkspacePath(resolveWorkspacePath(req.workspacePath(), savedParent.getId()));
            savedParent = platformTaskRepository.save(savedParent);
            created.add(PlatformTaskDto.from(savedParent));

            PlatformTask qa = createLoopStep(
                    savedParent,
                    title,
                    "QA",
                    "QA",
                    1,
                    "ASSIGNED",
                    "Iteration 1/" + maxIterations
                            + " — run Playwright E2E; set outcome PASS/FAIL/FLAKY before DONE."
            );
            created.add(PlatformTaskDto.from(qa));
            String startBody = "SYSTEM_E2E_LOOP started — begin QA iteration 1/" + maxIterations + ".";
            sendSwarmHandoff(qa, startBody);
            scheduleStepSessionInvoke(qa, startBody);
            writeE2eProgressMemory(savedParent, null, "STARTED",
                    "Started SYSTEM_E2E_LOOP maxIterations=" + maxIterations);
            upsertMemory(new UpsertPlatformMemoryRequest(
                    projectSlug,
                    "pipeline/" + pipeline.id() + "/" + savedParent.getId(),
                    "DECISION",
                    "Started looping pipeline " + pipeline.id() + " for \"" + title
                            + "\" (maxIterations=" + maxIterations + "). "
                            + "Flow: QA → (FAIL? FIX) → DOCS → REVIEW → retest until PASS or max."
            ));
            return created;
        }

        PlatformTask savedParent = platformTaskRepository.save(parent);
        savedParent.setWorkspacePath(resolveWorkspacePath(req.workspacePath(), savedParent.getId()));
        savedParent = platformTaskRepository.save(savedParent);
        created.add(PlatformTaskDto.from(savedParent));
        for (String stepRole : pipeline.steps()) {
            PlatformTask child = new PlatformTask();
            child.setTitle(title + " — " + stepRole);
            child.setDescription("Pipeline " + pipeline.id() + " step for " + stepRole);
            child.setRole(stepRole);
            child.setStatus("OPEN");
            child.setProjectSlug(projectSlug);
            child.setCreatedBy(user);
            child.setParentTaskId(savedParent.getId());
            child.setPipelineId(pipeline.id());
            PlatformTask savedChild = platformTaskRepository.save(child);
            savedChild.setWorkspacePath(resolveWorkspacePath(null, savedChild.getId()));
            created.add(PlatformTaskDto.from(platformTaskRepository.save(savedChild)));
        }

        upsertMemory(new UpsertPlatformMemoryRequest(
                projectSlug,
                "pipeline/" + pipeline.id() + "/" + savedParent.getId(),
                "DECISION",
                "Started pipeline " + pipeline.id() + " for \"" + title + "\" with "
                        + pipeline.steps().size() + " role steps."
        ));
        return created;
    }

    @Transactional(readOnly = true)
    public E2eLoopProgressDto getE2eLoopProgress(UUID runId) {
        PlatformTask parent = findTask(runId);
        if (parent.getParentTaskId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "runId must be the pipeline parent task id");
        }
        if (!PIPELINE_SYSTEM_E2E.equalsIgnoreCase(parent.getPipelineId() == null ? "" : parent.getPipelineId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Not a SYSTEM_E2E_LOOP run: " + parent.getPipelineId());
        }
        List<PlatformTask> children = platformTaskRepository.findByParentTaskIdOrderByCreatedAtAsc(runId);
        String lastQaOutcome = children.stream()
                .filter(c -> "QA".equals(c.getStepKey()))
                .reduce((a, b) -> b)
                .map(PlatformTask::getOutcome)
                .orElse(null);
        String stopReason = null;
        if ("DONE".equals(parent.getStatus())) {
            stopReason = "GREEN";
        } else if ("BLOCKED".equals(parent.getStatus())) {
            stopReason = "MAX_ITERATIONS";
        } else if ("CANCELLED".equals(parent.getStatus())) {
            stopReason = "CANCELLED";
        }
        List<E2eLoopProgressDto.StepDto> steps = children.stream()
                .map(c -> new E2eLoopProgressDto.StepDto(
                        c.getId(),
                        c.getIteration() == null ? 0 : c.getIteration(),
                        c.getStepKey(),
                        c.getRole(),
                        c.getStatus(),
                        c.getOutcome(),
                        c.getTitle(),
                        c.getUpdatedAt()
                ))
                .toList();
        return new E2eLoopProgressDto(
                parent.getId(),
                parent.getProjectSlug(),
                parent.getPipelineId(),
                parent.getStatus(),
                parent.getIteration() == null ? 1 : parent.getIteration(),
                parent.getMaxIterations() == null ? DEFAULT_E2E_MAX_ITERATIONS : parent.getMaxIterations(),
                lastQaOutcome,
                stopReason,
                steps,
                parent.getUpdatedAt()
        );
    }

    private SwarmAdvanceResult advancePipelineAfterDone(PlatformTask doneTask) {
        if (PIPELINE_SYSTEM_E2E.equalsIgnoreCase(
                doneTask.getPipelineId() == null ? "" : doneTask.getPipelineId())) {
            return advanceE2eLoopAfterDone(doneTask);
        }
        List<SwarmTickResultDto.SwarmActionDto> actions = new ArrayList<>();
        int advanced = 0;
        int parentsCompleted = 0;
        int messagesSent = 0;
        if (doneTask.getParentTaskId() == null) {
            return new SwarmAdvanceResult(0, 0, 0, actions);
        }
        List<PlatformTask> siblings = platformTaskRepository
                .findByParentTaskIdOrderByCreatedAtAsc(doneTask.getParentTaskId());
        PlatformTask next = null;
        boolean passedDone = false;
        for (PlatformTask sibling : siblings) {
            if (sibling.getId().equals(doneTask.getId())) {
                passedDone = true;
                continue;
            }
            if (passedDone && "OPEN".equals(sibling.getStatus())) {
                next = sibling;
                break;
            }
        }
        if (next != null) {
            next.setStatus("ASSIGNED");
            platformTaskRepository.save(next);
            sendSwarmHandoff(next, doneTask.getRole() + " marked DONE — your turn (" + next.getRole() + ").");
            advanced = 1;
            messagesSent = 1;
            actions.add(new SwarmTickResultDto.SwarmActionDto(
                    "HANDOFF", next.getId(), next.getRole(),
                    "Advanced after " + doneTask.getRole() + " completed"));
        }
        boolean allDone = siblings.stream().allMatch(s -> {
            if (s.getId().equals(doneTask.getId())) {
                return true;
            }
            return "DONE".equals(s.getStatus()) || "CANCELLED".equals(s.getStatus());
        });
        if (allDone) {
            PlatformTask parent = platformTaskRepository.findById(doneTask.getParentTaskId()).orElse(null);
            if (parent != null && !"DONE".equals(parent.getStatus()) && !"CANCELLED".equals(parent.getStatus())) {
                parent.setStatus("DONE");
                platformTaskRepository.save(parent);
                parentsCompleted = 1;
                actions.add(new SwarmTickResultDto.SwarmActionDto(
                        "PARENT_DONE", parent.getId(), parent.getRole(),
                        "All pipeline steps finished"));
                if (parent.getProjectSlug() != null) {
                    upsertMemory(new UpsertPlatformMemoryRequest(
                            parent.getProjectSlug(),
                            "pipeline/" + (parent.getPipelineId() == null ? "none" : parent.getPipelineId())
                                    + "/" + parent.getId() + "/complete",
                            "DECISION",
                            "Pipeline completed for \"" + parent.getTitle() + "\"."
                    ));
                }
            }
        }
        return new SwarmAdvanceResult(advanced, parentsCompleted, messagesSent, actions);
    }

    private SwarmAdvanceResult advanceE2eLoopAfterDone(PlatformTask doneTask) {
        List<SwarmTickResultDto.SwarmActionDto> actions = new ArrayList<>();
        int advanced = 0;
        int parentsCompleted = 0;
        int messagesSent = 0;
        if (doneTask.getParentTaskId() == null) {
            return new SwarmAdvanceResult(0, 0, 0, actions);
        }
        PlatformTask parent = platformTaskRepository.findById(doneTask.getParentTaskId()).orElse(null);
        if (parent == null) {
            return new SwarmAdvanceResult(0, 0, 0, actions);
        }
        String stepKey = doneTask.getStepKey() == null ? inferE2eStepKey(doneTask.getRole()) : doneTask.getStepKey();
        int iteration = doneTask.getIteration() == null
                ? (parent.getIteration() == null ? 1 : parent.getIteration())
                : doneTask.getIteration();
        int maxIterations = parent.getMaxIterations() == null
                ? DEFAULT_E2E_MAX_ITERATIONS
                : parent.getMaxIterations();

        switch (stepKey) {
            case "QA" -> {
                String outcome = doneTask.getOutcome();
                if (outcome == null || outcome.isBlank()) {
                    outcome = "FAIL";
                    doneTask.setOutcome(outcome);
                    platformTaskRepository.save(doneTask);
                }
                writeE2eProgressMemory(parent, outcome, "QA_" + outcome,
                        "Iteration " + iteration + " QA outcome=" + outcome);
                if ("PASS".equals(outcome)) {
                    PlatformTask docs = createLoopStep(
                            parent,
                            parent.getTitle(),
                            "DOCS",
                            "PRODUCT",
                            iteration,
                            "ASSIGNED",
                            "E2E green on iteration " + iteration
                                    + " — update docs/evidence. Commit/push still require REVIEW human gate."
                    );
                    String docsBody = "QA PASS on iteration " + iteration
                            + " — update docs and evidence, then hand to REVIEW.";
                    sendSwarmHandoff(docs, docsBody);
                    scheduleStepSessionInvoke(docs, docsBody);
                    advanced = 1;
                    messagesSent = 1;
                    actions.add(new SwarmTickResultDto.SwarmActionDto(
                            "E2E_GREEN_DOCS", docs.getId(), docs.getRole(),
                            "QA passed — docs wrap-up before review"));
                } else {
                    PlatformTask fix = createLoopStep(
                            parent,
                            parent.getTitle(),
                            "FIX",
                            "BACKEND",
                            iteration,
                            "ASSIGNED",
                            "QA " + outcome + " on iteration " + iteration
                                    + " — fix product bugs from QA report; message FRONTEND if UI-owned. "
                                    + "Do not weaken tests to force green."
                    );
                    String qaReport = doneTask.getDescription() == null ? "(no QA description)" : doneTask.getDescription();
                    String fixBody = "QA reported " + outcome + " (iteration " + iteration + ").\n\n" + qaReport;
                    sendSwarmHandoff(fix, fixBody);
                    scheduleStepSessionInvoke(fix, fixBody);
                    advanced = 1;
                    messagesSent = 1;
                    actions.add(new SwarmTickResultDto.SwarmActionDto(
                            "E2E_FAIL_FIX", fix.getId(), fix.getRole(),
                            "QA failed — assigned fixer"));
                }
            }
            case "FIX" -> {
                PlatformTask docs = createLoopStep(
                        parent,
                        parent.getTitle(),
                        "DOCS",
                        "PRODUCT",
                        iteration,
                        "ASSIGNED",
                        "Document fixes for iteration " + iteration + " and point to evidence paths."
                );
                String docsBody = "FIX done on iteration " + iteration + " — update docs, then REVIEW.";
                sendSwarmHandoff(docs, docsBody);
                scheduleStepSessionInvoke(docs, docsBody);
                advanced = 1;
                messagesSent = 1;
                actions.add(new SwarmTickResultDto.SwarmActionDto(
                        "E2E_DOCS", docs.getId(), docs.getRole(),
                        "Fix complete — docs next"));
            }
            case "DOCS" -> {
                PlatformTask review = createLoopStep(
                        parent,
                        parent.getTitle(),
                        "REVIEW",
                        "SECURITY",
                        iteration,
                        "ASSIGNED",
                        "Human gate for iteration " + iteration
                                + ": approve commit/push only after review. Do not auto-push."
                );
                String reviewBody = "DOCS done on iteration " + iteration
                        + " — review changes; human must approve commit/push.";
                sendSwarmHandoff(review, reviewBody);
                scheduleStepSessionInvoke(review, reviewBody);
                advanced = 1;
                messagesSent = 1;
                actions.add(new SwarmTickResultDto.SwarmActionDto(
                        "E2E_REVIEW", review.getId(), review.getRole(),
                        "Docs complete — human review/commit gate"));
            }
            case "REVIEW" -> {
                String qaOutcome = findLatestQaOutcome(parent.getId(), iteration);
                if ("PASS".equals(qaOutcome)) {
                    parent.setStatus("DONE");
                    platformTaskRepository.save(parent);
                    parentsCompleted = 1;
                    writeE2eProgressMemory(parent, qaOutcome, "GREEN",
                            "Stopped green after review on iteration " + iteration);
                    upsertMemory(new UpsertPlatformMemoryRequest(
                            parent.getProjectSlug(),
                            "pipeline/" + PIPELINE_SYSTEM_E2E + "/" + parent.getId() + "/complete",
                            "DECISION",
                            "SYSTEM_E2E_LOOP GREEN for \"" + parent.getTitle()
                                    + "\" at iteration " + iteration + "/" + maxIterations + "."
                    ));
                    actions.add(new SwarmTickResultDto.SwarmActionDto(
                            "E2E_GREEN", parent.getId(), parent.getRole(),
                            "All E2E passed — run complete"));
                } else if (iteration >= maxIterations) {
                    parent.setStatus("BLOCKED");
                    platformTaskRepository.save(parent);
                    writeE2eProgressMemory(parent, qaOutcome, "MAX_ITERATIONS",
                            "Blocked after " + maxIterations + " iterations without green");
                    upsertMemory(new UpsertPlatformMemoryRequest(
                            parent.getProjectSlug(),
                            "pipeline/" + PIPELINE_SYSTEM_E2E + "/" + parent.getId() + "/blocked",
                            "DECISION",
                            "SYSTEM_E2E_LOOP BLOCKED at maxIterations=" + maxIterations
                                    + " for \"" + parent.getTitle() + "\"."
                    ));
                    actions.add(new SwarmTickResultDto.SwarmActionDto(
                            "E2E_MAX_BLOCKED", parent.getId(), parent.getRole(),
                            "Max iterations reached without green"));
                } else {
                    int nextIteration = iteration + 1;
                    parent.setIteration(nextIteration);
                    platformTaskRepository.save(parent);
                    PlatformTask qa = createLoopStep(
                            parent,
                            parent.getTitle(),
                            "QA",
                            "QA",
                            nextIteration,
                            "ASSIGNED",
                            "Iteration " + nextIteration + "/" + maxIterations
                                    + " — re-run Playwright E2E after fixes; set outcome before DONE."
                    );
                    String qaBody = "Iteration " + nextIteration + " — retest after review of iteration "
                            + iteration + ".";
                    sendSwarmHandoff(qa, qaBody);
                    scheduleStepSessionInvoke(qa, qaBody);
                    writeE2eProgressMemory(parent, qaOutcome, "LOOP_NEXT",
                            "Advanced to iteration " + nextIteration);
                    advanced = 1;
                    messagesSent = 1;
                    actions.add(new SwarmTickResultDto.SwarmActionDto(
                            "E2E_LOOP_NEXT", qa.getId(), qa.getRole(),
                            "Starting iteration " + nextIteration));
                }
            }
            default -> actions.add(new SwarmTickResultDto.SwarmActionDto(
                    "E2E_UNKNOWN_STEP", doneTask.getId(), doneTask.getRole(),
                    "Unknown stepKey " + stepKey + " — no handoff"));
        }
        return new SwarmAdvanceResult(advanced, parentsCompleted, messagesSent, actions);
    }

    private PlatformTask createLoopStep(
            PlatformTask parent,
            String title,
            String stepKey,
            String role,
            int iteration,
            String status,
            String description
    ) {
        PlatformTask child = new PlatformTask();
        child.setTitle(title + " — " + stepKey + " #" + iteration);
        child.setDescription(description);
        child.setRole(role);
        child.setStatus(status);
        child.setProjectSlug(parent.getProjectSlug());
        child.setCreatedBy(CurrentUser.usernameOrAnonymous());
        child.setParentTaskId(parent.getId());
        child.setPipelineId(PIPELINE_SYSTEM_E2E);
        child.setIteration(iteration);
        child.setMaxIterations(parent.getMaxIterations());
        child.setStepKey(stepKey);
        PlatformTask saved = platformTaskRepository.save(child);
        saved.setWorkspacePath(resolveWorkspacePath(parent.getWorkspacePath(), saved.getId()));
        return platformTaskRepository.save(saved);
    }

    private String findLatestQaOutcome(UUID parentId, int iteration) {
        return platformTaskRepository.findByParentTaskIdOrderByCreatedAtAsc(parentId).stream()
                .filter(c -> "QA".equals(c.getStepKey())
                        && c.getIteration() != null
                        && c.getIteration() == iteration)
                .map(PlatformTask::getOutcome)
                .filter(o -> o != null && !o.isBlank())
                .reduce((a, b) -> b)
                .orElse("FAIL");
    }

    private String inferE2eStepKey(String role) {
        if (role == null) {
            return "QA";
        }
        return switch (role) {
            case "QA" -> "QA";
            case "BACKEND", "FRONTEND" -> "FIX";
            case "PRODUCT" -> "DOCS";
            case "SECURITY" -> "REVIEW";
            default -> "QA";
        };
    }

    private int resolveMaxIterations(PlatformPipelineDto pipeline, Integer requested) {
        if (requested != null) {
            return requested;
        }
        if (pipeline.maxIterations() != null) {
            return pipeline.maxIterations();
        }
        return DEFAULT_E2E_MAX_ITERATIONS;
    }

    private void writeE2eProgressMemory(
            PlatformTask parent,
            String lastQaOutcome,
            String event,
            String detail
    ) {
        if (parent.getProjectSlug() == null || parent.getProjectSlug().isBlank()) {
            return;
        }
        int iteration = parent.getIteration() == null ? 1 : parent.getIteration();
        int maxIterations = parent.getMaxIterations() == null
                ? DEFAULT_E2E_MAX_ITERATIONS
                : parent.getMaxIterations();
        String qa = lastQaOutcome == null ? "null" : "\"" + lastQaOutcome + "\"";
        String value = """
                {
                  "runId": "%s",
                  "pipelineId": "%s",
                  "status": "%s",
                  "iteration": %d,
                  "maxIterations": %d,
                  "lastQaOutcome": %s,
                  "event": "%s",
                  "detail": "%s",
                  "updatedAt": "%s"
                }
                """.formatted(
                parent.getId(),
                PIPELINE_SYSTEM_E2E,
                parent.getStatus(),
                iteration,
                maxIterations,
                qa,
                event.replace("\"", "'"),
                detail.replace("\"", "'"),
                Instant.now()
        ).trim();
        upsertMemory(new UpsertPlatformMemoryRequest(
                parent.getProjectSlug(),
                "e2e-loop/" + parent.getId() + "/progress",
                "PROGRESS",
                value
        ));
    }

    private void scheduleStepSessionInvoke(PlatformTask task, String handoffBody) {
        UUID taskId = task.getId();
        Runnable run = () -> {
            try {
                PlatformTask latest = platformTaskRepository.findById(taskId).orElse(null);
                if (latest == null) {
                    return;
                }
                if (latest.getSessionId() != null) {
                    log.info("Skip auto-invoke for task {} — already linked to session {}",
                            taskId, latest.getSessionId());
                    return;
                }
                SessionDto session = invokeStepSessionNow(latest, handoffBody);
                log.info("Auto-invoked session {} for task {} role {} step {}",
                        session.id(), taskId, latest.getRole(), latest.getStepKey());
            } catch (Exception e) {
                log.warn("Auto-invoke session failed for task {}: {}", taskId, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    run.run();
                }
            });
        } else {
            run.run();
        }
    }

    private SessionDto invokeStepSessionNow(PlatformTask task, String handoffBody) throws Exception {
        String workspace = sessionWorkspaceFor(task);
        SessionDto created = sessionService.create(new CreateSessionRequest(
                task.getTitle(),
                workspace,
                "cursor",
                true,
                task.getRole(),
                task.getId()
        ));
        task.setSessionId(created.id());
        if ("OPEN".equals(task.getStatus())) {
            task.setStatus("ASSIGNED");
        }
        platformTaskRepository.save(task);
        String prompt = buildE2eStepPrompt(task, handoffBody);
        sessionService.prompt(created.id(), new PromptRequest(prompt));
        return sessionService.get(created.id());
    }

    private String sessionWorkspaceFor(PlatformTask task) {
        Path root = workspacePathResolver.root();
        String wp = task.getWorkspacePath();
        if (wp != null && !wp.isBlank()) {
            Path p = Path.of(wp).toAbsolutePath().normalize();
            if (WorkspacePathResolver.isUnder(p, root)) {
                return root.relativize(p).toString().replace('\\', '/');
            }
            if (workspacePathResolver.isAllowed(p)) {
                return p.toString();
            }
        }
        return "agent-api";
    }

    private String buildE2eStepPrompt(PlatformTask task, String handoffBody) {
        String step = task.getStepKey() == null ? "" : task.getStepKey();
        String skill = switch (step) {
            case "QA" -> "Load skill ap-platform-qa (and ap-e2e-realme-p2-pro for Realme). "
                    + "When finished, PATCH this platform task status=DONE with outcome PASS|FAIL|FLAKY "
                    + "and put the report in description. Task id: " + task.getId();
            case "FIX" -> "Load skill ap-platform-em / implement fixes from the QA report. "
                    + "Do not weaken tests. When done, PATCH task " + task.getId() + " status=DONE.";
            case "DOCS" -> "Update docs/evidence for this E2E iteration. When done, PATCH task "
                    + task.getId() + " status=DONE.";
            case "REVIEW" -> "Load skill ap-platform-review. This is the human commit/push gate "
                    + "(role SECURITY, humanApprovalRequired). Review Changes; do NOT auto-push. "
                    + "When the human approves (or you confirm no push needed), PATCH task "
                    + task.getId() + " status=DONE. Progress: GET /api/platform/pipelines/runs/{parentRunId}.";
            default -> "Complete platform task " + task.getId() + " then mark DONE.";
        };
        return "[SYSTEM_E2E_LOOP handoff — step " + step + " / role " + task.getRole() + "]\n"
                + handoffBody + "\n\n" + skill;
    }

    private void sendSwarmHandoff(PlatformTask target, String body) {
        String projectSlug = target.getProjectSlug() == null ? "default" : target.getProjectSlug();
        PlatformAgentMessage msg = new PlatformAgentMessage();
        msg.setProjectSlug(projectSlug);
        msg.setTaskId(target.getId());
        msg.setFromRole("EM");
        msg.setToRole(target.getRole());
        msg.setSubject("Handoff: " + target.getTitle());
        msg.setBody(body);
        msg.setStatus("UNREAD");
        msg.setCreatedBy(CurrentUser.usernameOrAnonymous());
        platformAgentMessageRepository.save(msg);
    }

    private record SwarmAdvanceResult(
            int advanced,
            int parentsCompleted,
            int messagesSent,
            List<SwarmTickResultDto.SwarmActionDto> actions
    ) {
    }

    private PlatformTask findTask(UUID id) {
        return platformTaskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    private void requireSession(UUID sessionId) {
        if (!agentSessionRepository.existsById(sessionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId not found");
        }
    }

    private String resolveWorkspacePath(String requested, UUID taskId) {
        if (requested == null || requested.isBlank()) {
            return workspacePathResolver.root()
                    .resolve("platform-tasks")
                    .resolve(taskId.toString())
                    .normalize()
                    .toString();
        }
        try {
            return workspacePathResolver.resolve(requested).toString();
        } catch (IllegalArgumentException | SecurityException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private String normalizeOptionalRole(String role) {
        return role == null || role.isBlank() ? null : normalizeRole(role);
    }

    private String normalizeOptionalStatus(String status) {
        return status == null || status.isBlank() ? null : normalizeStatus(status);
    }

    private String normalizeOptionalMemoryKind(String kind) {
        return kind == null || kind.isBlank() ? null : normalizeMemoryKind(kind);
    }

    private String normalizeOptionalMessageRole(String role) {
        return role == null || role.isBlank() ? null : normalizeMessageRole(role);
    }

    private String normalizeRole(String role) {
        String normalized = requireText(role, "role").toUpperCase(Locale.ROOT);
        if (!ROLES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + role);
        }
        return normalized;
    }

    private String normalizeMessageRole(String role) {
        String normalized = requireText(role, "role").toUpperCase(Locale.ROOT);
        if (!MESSAGE_ROLES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid message role: " + role);
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        String normalized = requireText(status, "status").toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + status);
        }
        return normalized;
    }

    private String normalizeMemoryKind(String kind) {
        String normalized = requireText(kind, "kind").toUpperCase(Locale.ROOT);
        if (!MEMORY_KINDS.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid memory kind: " + kind);
        }
        return normalized;
    }

    private String normalizeOutcome(String outcome) {
        String normalized = requireText(outcome, "outcome").toUpperCase(Locale.ROOT);
        if (!OUTCOMES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid outcome: " + outcome);
        }
        return normalized;
    }

    private String normalizeStepKey(String stepKey) {
        String normalized = requireText(stepKey, "stepKey").toUpperCase(Locale.ROOT);
        if (!STEP_KEYS.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid stepKey: " + stepKey);
        }
        return normalized;
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
