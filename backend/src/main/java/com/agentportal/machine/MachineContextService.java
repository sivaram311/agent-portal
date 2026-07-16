package com.agentportal.machine;

import com.agentportal.config.AgentProperties;
import com.agentportal.config.AppProperties;
import com.agentportal.config.CssProperties;
import com.agentportal.domain.AgentSession;
import com.agentportal.domain.SessionStatus;
import com.agentportal.dto.PlatformOrgDto;
import com.agentportal.dto.PlatformTaskDto;
import com.agentportal.dto.PortLeaseDto;
import com.agentportal.repo.AgentSessionRepository;
import com.agentportal.service.PlatformRegistryService;
import com.agentportal.service.WorkspacePathResolver;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MachineContextService {

    private final AppProperties appProperties;
    private final AgentProperties agentProperties;
    private final CssProperties cssProperties;
    private final PlatformRegistryService platformRegistryService;
    private final AgentSessionRepository sessionRepository;
    private final WorkspacePathResolver workspacePathResolver;

    public MachineContextService(
            AppProperties appProperties,
            AgentProperties agentProperties,
            CssProperties cssProperties,
            PlatformRegistryService platformRegistryService,
            AgentSessionRepository sessionRepository,
            WorkspacePathResolver workspacePathResolver
    ) {
        this.appProperties = appProperties;
        this.agentProperties = agentProperties;
        this.cssProperties = cssProperties;
        this.platformRegistryService = platformRegistryService;
        this.sessionRepository = sessionRepository;
        this.workspacePathResolver = workspacePathResolver;
    }

    public Map<String, Object> buildContext() {
        AppProperties.MachineGateway gw = appProperties.getMachineGateway();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("generatedAt", Instant.now().toString());
        body.put("ttlSeconds", gw.getTtlSeconds());

        Map<String, Object> host = new LinkedHashMap<>();
        host.put("controlPlane", "E:\\MyWorkspace");
        host.put("standingOrders", "E:\\MyAgent\\workflow");
        host.put("publicZone", "delena.buzz");
        host.put("osName", System.getProperty("os.name"));
        host.put("osVersion", System.getProperty("os.version"));
        host.put("agentWorkspaceRoot", agentProperties.getWorkspace().getRoot());
        body.put("host", host);

        Map<String, Object> env = new LinkedHashMap<>();
        env.put("default", "DEV");
        env.put("urls", Map.of(
                "devUi", "https://delena.buzz",
                "devApi", "https://delena.buzz/api",
                "css", cssProperties.isEnabled() ? cssProperties.getAuthUrl() : "disabled"
        ));
        body.put("env", env);

        Map<String, Object> docs = new LinkedHashMap<>();
        docs.put("agentsEntry", "E:\\MyWorkspace\\AGENTS.md");
        docs.put("platformHandbook", "agent-portal/docs/platform/");
        docs.put("machineGatewayIdea", "machine-gateway/docs/IDEA.md");
        docs.put("indexes", List.of(
                "agent-portal/docs/platform/indexes/INDEX-MYWORKSPACE.md",
                "agent-portal/docs/platform/indexes/INDEX-SOURCE.md"
        ));
        body.put("docs", docs);

        List<PortLeaseDto> leases = platformRegistryService.listPorts(true);
        List<Map<String, Object>> leaseMaps = new ArrayList<>();
        for (PortLeaseDto p : leases) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", p.id() == null ? null : p.id().toString());
            row.put("port", p.port());
            row.put("service", p.service());
            row.put("ownerApp", p.ownerApp());
            row.put("env", p.env());
            row.put("bindAddress", p.bindAddress());
            row.put("status", p.status());
            row.put("claimedBy", p.claimedBy());
            row.put("pid", null);
            row.put("pidHint", "Resolve with Get-NetTCPConnection -LocalPort " + p.port());
            leaseMaps.add(row);
        }
        body.put("ports", Map.of(
                "source", "/api/platform/ports",
                "leases", leaseMaps
        ));

        List<AgentSession> sessions = sessionRepository.findByStatusNotOrderByUpdatedAtDesc(SessionStatus.ARCHIVED);
        List<Map<String, Object>> sessionItems = new ArrayList<>();
        int limit = Math.min(sessions.size(), 25);
        for (int i = 0; i < limit; i++) {
            AgentSession s = sessions.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", s.getId().toString());
            row.put("title", s.getTitle());
            row.put("status", s.getStatus().name());
            row.put("provider", s.getProvider());
            row.put("platformRole", s.getPlatformRole());
            row.put("workspacePath", s.getWorkspacePath());
            row.put("updatedAt", s.getUpdatedAt() == null ? null : s.getUpdatedAt().toString());
            sessionItems.add(row);
        }
        body.put("sessions", Map.of(
                "activeCount", sessions.size(),
                "items", sessionItems
        ));

        List<PlatformTaskDto> openTasks = platformRegistryService.listTasks("OPEN", null);
        List<PlatformTaskDto> assigned = platformRegistryService.listTasks("ASSIGNED", null);
        List<PlatformTaskDto> inProgress = platformRegistryService.listTasks("IN_PROGRESS", null);
        PlatformOrgDto org = platformRegistryService.orgDashboard();
        Map<String, Object> platform = new LinkedHashMap<>();
        platform.put("openTasks", summarizeTasks(openTasks));
        platform.put("assignedTasks", summarizeTasks(assigned));
        platform.put("inProgressTasks", summarizeTasks(inProgress));
        platform.put("runningPipelines", List.of());
        platform.put("recentMessages", platformRegistryService.listMessages(null, null, null).stream()
                .limit(10)
                .map(m -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", m.id() == null ? null : m.id().toString());
                    row.put("fromRole", m.fromRole());
                    row.put("toRole", m.toRole());
                    row.put("subject", m.subject());
                    row.put("createdAt", m.createdAt() == null ? null : m.createdAt().toString());
                    return row;
                })
                .toList());
        platform.put("orgSummary", Map.of(
                "taskCountsByStatus", org.tasksByStatus(),
                "taskCountsByRole", org.tasksByRole(),
                "linkedSessions", org.linkedSessions(),
                "activeProjects", org.activeProjects()
        ));
        body.put("platform", platform);

        Map<String, Object> health = new LinkedHashMap<>();
        String cursorCmd = agentProperties.getCursor().getCommand();
        health.put("portalApi", "ok");
        health.put("css", cssProperties.isEnabled() ? "enabled" : "disabled");
        health.put("cursorCommandExists", Files.exists(Path.of(cursorCmd)) || "agent".equalsIgnoreCase(cursorCmd));
        health.put("apiKeyConfigured", agentProperties.getCursor().getApiKey() != null
                && !agentProperties.getCursor().getApiKey().isBlank());
        body.put("health", health);

        Map<String, Object> rules = new LinkedHashMap<>();
        rules.put("killByPortOnly", true);
        rules.put("sandboxRoot", gw.getSandboxRoot());
        rules.put("gatewayWorkspace", gw.getWorkspacePath());
        rules.put("resolvedGatewayWorkspace", workspacePathResolver.resolve(gw.getWorkspacePath()).toString());
        rules.put("noSecondIdp", true);
        rules.put("maxMode", gw.getMaxMode());
        body.put("rules", rules);

        body.put("modes", List.of("observe", "advise", "act", "ops"));

        @SuppressWarnings("unchecked")
        Map<String, Object> redacted = SecretRedactor.redactMap(body);
        return redacted;
    }

    private static List<Map<String, Object>> summarizeTasks(List<PlatformTaskDto> tasks) {
        List<Map<String, Object>> out = new ArrayList<>();
        int n = Math.min(tasks.size(), 20);
        for (int i = 0; i < n; i++) {
            PlatformTaskDto t = tasks.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", t.id() == null ? null : t.id().toString());
            row.put("title", t.title());
            row.put("status", t.status());
            row.put("role", t.role());
            out.add(row);
        }
        return out;
    }
}
