package com.agentportal.service;

import com.agentportal.config.AgentProperties;
import com.agentportal.domain.PlatformAgentMessage;
import com.agentportal.domain.PlatformApp;
import com.agentportal.domain.PlatformMemoryEntry;
import com.agentportal.domain.PlatformTask;
import com.agentportal.domain.PortLease;
import com.agentportal.dto.ClaimPortRequest;
import com.agentportal.dto.CreatePlatformAgentMessageRequest;
import com.agentportal.dto.CreatePlatformTaskRequest;
import com.agentportal.dto.LinkTaskSessionRequest;
import com.agentportal.dto.PlatformAgentMessageDto;
import com.agentportal.dto.PlatformAppDto;
import com.agentportal.dto.PlatformMemoryDto;
import com.agentportal.dto.PlatformPipelineDto;
import com.agentportal.dto.PlatformRoleDto;
import com.agentportal.dto.PlatformTaskDto;
import com.agentportal.dto.PortLeaseDto;
import com.agentportal.dto.RunPlatformPipelineRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    private static final Set<String> ROLES = Set.of(
            "ARCHITECTURE", "BACKEND", "FRONTEND", "QA", "DEVOPS", "PRODUCT", "SECURITY");
    private static final Set<String> MESSAGE_ROLES = Set.of(
            "ARCHITECTURE", "BACKEND", "FRONTEND", "QA", "DEVOPS", "PRODUCT", "SECURITY", "EM", "*");
    private static final Set<String> STATUSES = Set.of(
            "OPEN", "ASSIGNED", "IN_PROGRESS", "BLOCKED", "DONE", "CANCELLED");
    private static final Set<String> MEMORY_KINDS = Set.of(
            "NOTE", "DECISION", "CONTRACT", "ARTIFACT", "MESSAGE_SUMMARY");
    private static final List<PlatformRoleDto> ROLE_DTOS = List.of(
            new PlatformRoleDto("ARCHITECTURE", "Architecture", "Engineering",
                    "sandbox/platform-tasks/architecture", "ap-platform-em / architecture prompts"),
            new PlatformRoleDto("BACKEND", "Backend", "Engineering",
                    "sandbox/platform-tasks/backend", "ap-platform-em / backend prompts"),
            new PlatformRoleDto("FRONTEND", "Frontend", "Engineering",
                    "sandbox/platform-tasks/frontend", "ap-platform-em / frontend prompts"),
            new PlatformRoleDto("QA", "QA", "Engineering",
                    "sandbox/platform-tasks/qa", "ap-platform-em / qa prompts"),
            new PlatformRoleDto("DEVOPS", "DevOps", "Engineering",
                    "sandbox/platform-tasks/devops", "ap-platform-em / devops prompts"),
            new PlatformRoleDto("PRODUCT", "Product", "Product",
                    "sandbox/platform-tasks/product", "ap-platform-em / product prompts"),
            new PlatformRoleDto("SECURITY", "Security", "Security",
                    "sandbox/platform-tasks/security", "ap-platform-em / security prompts")
    );
    private static final Map<String, PlatformPipelineDto> PIPELINES = new LinkedHashMap<>();

    static {
        PIPELINES.put("FEATURE", new PlatformPipelineDto(
                "FEATURE",
                "Feature delivery",
                "Clarify → design → implement → verify → deploy proposal",
                List.of("PRODUCT", "ARCHITECTURE", "BACKEND", "FRONTEND", "QA", "DEVOPS")));
        PIPELINES.put("BUGFIX", new PlatformPipelineDto(
                "BUGFIX",
                "Bug fix",
                "Reproduce → fix → verify",
                List.of("BACKEND", "FRONTEND", "QA")));
        PIPELINES.put("REFACTOR", new PlatformPipelineDto(
                "REFACTOR",
                "Refactor",
                "Design → change → regression QA",
                List.of("ARCHITECTURE", "BACKEND", "FRONTEND", "QA")));
        PIPELINES.put("SECURITY_AUDIT", new PlatformPipelineDto(
                "SECURITY_AUDIT",
                "Security audit",
                "Audit → harden → retest",
                List.of("SECURITY", "BACKEND", "QA")));
    }

    private final PortLeaseRepository portLeaseRepository;
    private final PlatformAppRepository platformAppRepository;
    private final PlatformTaskRepository platformTaskRepository;
    private final PlatformMemoryRepository platformMemoryRepository;
    private final PlatformAgentMessageRepository platformAgentMessageRepository;
    private final AgentSessionRepository agentSessionRepository;
    private final AgentProperties agentProperties;

    public PlatformRegistryService(
            PortLeaseRepository portLeaseRepository,
            PlatformAppRepository platformAppRepository,
            PlatformTaskRepository platformTaskRepository,
            PlatformMemoryRepository platformMemoryRepository,
            PlatformAgentMessageRepository platformAgentMessageRepository,
            AgentSessionRepository agentSessionRepository,
            AgentProperties agentProperties
    ) {
        this.portLeaseRepository = portLeaseRepository;
        this.platformAppRepository = platformAppRepository;
        this.platformTaskRepository = platformTaskRepository;
        this.platformMemoryRepository = platformMemoryRepository;
        this.platformAgentMessageRepository = platformAgentMessageRepository;
        this.agentSessionRepository = agentSessionRepository;
        this.agentProperties = agentProperties;
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
        PlatformTask saved = platformTaskRepository.save(task);
        saved.setWorkspacePath(resolveWorkspacePath(req.workspacePath(), saved.getId()));
        return PlatformTaskDto.from(platformTaskRepository.save(saved));
    }

    @Transactional
    public PlatformTaskDto updateTask(UUID id, UpdatePlatformTaskRequest req) {
        PlatformTask task = findTask(id);
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
        return PlatformTaskDto.from(platformTaskRepository.save(task));
    }

    @Transactional
    public PlatformTaskDto linkTaskSession(UUID id, LinkTaskSessionRequest req) {
        PlatformTask task = findTask(id);
        requireSession(req.sessionId());
        task.setSessionId(req.sessionId());
        if ("OPEN".equals(task.getStatus())) {
            task.setStatus("ASSIGNED");
        }
        return PlatformTaskDto.from(platformTaskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<PlatformRoleDto> listRoles() {
        return ROLE_DTOS;
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
        PlatformTask savedParent = platformTaskRepository.save(parent);
        savedParent.setWorkspacePath(resolveWorkspacePath(req.workspacePath(), savedParent.getId()));
        savedParent = platformTaskRepository.save(savedParent);

        List<PlatformTaskDto> created = new ArrayList<>();
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
        Path root = Path.of(agentProperties.getWorkspace().getRoot()).toAbsolutePath().normalize();
        if (requested == null || requested.isBlank()) {
            return root.resolve("platform-tasks").resolve(taskId.toString()).normalize().toString();
        }
        String trimmed = requested.trim();
        if (trimmed.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "workspacePath must not contain '..'");
        }
        boolean absolute = Path.of(trimmed).isAbsolute()
                || trimmed.contains(":")
                || trimmed.startsWith("/")
                || trimmed.startsWith("\\");
        Path candidate = absolute
                ? Path.of(trimmed).toAbsolutePath().normalize()
                : root.resolve(trimmed).toAbsolutePath().normalize();
        if (!candidate.startsWith(root)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "workspacePath must stay under " + root);
        }
        return candidate.toString();
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
