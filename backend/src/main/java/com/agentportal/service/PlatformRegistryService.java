package com.agentportal.service;

import com.agentportal.config.AgentProperties;
import com.agentportal.domain.PlatformApp;
import com.agentportal.domain.PlatformTask;
import com.agentportal.domain.PortLease;
import com.agentportal.dto.ClaimPortRequest;
import com.agentportal.dto.CreatePlatformTaskRequest;
import com.agentportal.dto.PlatformAppDto;
import com.agentportal.dto.PlatformRoleDto;
import com.agentportal.dto.PlatformTaskDto;
import com.agentportal.dto.PortLeaseDto;
import com.agentportal.dto.UpdatePlatformTaskRequest;
import com.agentportal.repo.PlatformAppRepository;
import com.agentportal.repo.PlatformTaskRepository;
import com.agentportal.repo.PortLeaseRepository;
import com.agentportal.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class PlatformRegistryService {

    private static final Set<String> ROLES = Set.of(
            "ARCHITECTURE", "BACKEND", "FRONTEND", "QA", "DEVOPS", "PRODUCT", "SECURITY");
    private static final Set<String> STATUSES = Set.of(
            "OPEN", "ASSIGNED", "IN_PROGRESS", "BLOCKED", "DONE", "CANCELLED");
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

    private final PortLeaseRepository portLeaseRepository;
    private final PlatformAppRepository platformAppRepository;
    private final PlatformTaskRepository platformTaskRepository;
    private final AgentProperties agentProperties;

    public PlatformRegistryService(
            PortLeaseRepository portLeaseRepository,
            PlatformAppRepository platformAppRepository,
            PlatformTaskRepository platformTaskRepository,
            AgentProperties agentProperties
    ) {
        this.portLeaseRepository = portLeaseRepository;
        this.platformAppRepository = platformAppRepository;
        this.platformTaskRepository = platformTaskRepository;
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
        task.setSessionId(req.sessionId());
        // Persist first so @GeneratedValue assigns id (do not setId manually).
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
            task.setSessionId(req.sessionId());
        }
        return PlatformTaskDto.from(platformTaskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<PlatformRoleDto> listRoles() {
        return ROLE_DTOS;
    }

    private PlatformTask findTask(UUID id) {
        return platformTaskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    private String resolveWorkspacePath(String requested, UUID taskId) {
        Path root = Path.of(agentProperties.getWorkspace().getRoot()).toAbsolutePath().normalize();
        if (requested == null || requested.isBlank()) {
            // workspace.root is already the sandbox root in Phase 1
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

    private String normalizeRole(String role) {
        String normalized = requireText(role, "role").toUpperCase(Locale.ROOT);
        if (!ROLES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + role);
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
