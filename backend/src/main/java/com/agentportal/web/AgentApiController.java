package com.agentportal.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Discovery surface for external AIs — lists UI-parity actions.
 * Auth: public catalog; mutating session APIs remain under /api/sessions/** with JWT/API-key.
 */
@RestController
@RequestMapping("/api/agent")
public class AgentApiController {

    @GetMapping(value = "/actions", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> actions() {
        return Map.of(
                "name", "agent-portal-agent-api",
                "version", "1.0",
                "auth", List.of("Bearer CSS JWT (clientId=agent-portal)", "X-API-Key when configured"),
                "cors", "any origin (APP_CORS_ORIGINS=* by default); auth still required for /api/**",
                "docs", List.of(
                        "/workspaces/agent-api/ACTIONS.md",
                        "/workspaces/agent-api/openapi/agent-api.yaml",
                        "/docs/platform/AGENT-API.md"
                ),
                "defaultWorkspacePath", "agent-api",
                "actions", List.of(
                        action("health", "GET", "/api/health", "Capability probe"),
                        action("authConfig", "GET", "/api/auth/config", "Whether CSS is required"),
                        action("listPresets", "GET", "/api/presets", "Session presets"),
                        action("listSessions", "GET", "/api/sessions", "List sessions"),
                        action("createSession", "POST", "/api/sessions", "New session (UI create dialog)"),
                        action("getSession", "GET", "/api/sessions/{id}", "Open session"),
                        action("listMessages", "GET", "/api/sessions/{id}/messages", "Transcript"),
                        action("listTools", "GET", "/api/sessions/{id}/tools", "Tool runs / Logs"),
                        action("listPermissions", "GET", "/api/sessions/{id}/permissions", "Pending permissions"),
                        action("sendPrompt", "POST", "/api/sessions/{id}/prompt", "Send chat prompt"),
                        action("cancelRun", "POST", "/api/sessions/{id}/cancel", "Cancel in-flight run"),
                        action("resolvePermission", "POST", "/api/sessions/{id}/permissions/{permissionId}", "Allow/deny"),
                        action("abandonSubagent", "POST", "/api/sessions/{id}/subagents/{subId}/abandon", "Abandon child"),
                        action("archiveSession", "POST", "/api/sessions/{id}/archive", "Archive"),
                        action("unarchiveSession", "POST", "/api/sessions/{id}/unarchive", "Unarchive"),
                        action("listFiles", "GET", "/api/sessions/{id}/files", "Code tab tree"),
                        action("readFile", "GET", "/api/sessions/{id}/files/content", "Read file"),
                        action("listChanges", "GET", "/api/sessions/{id}/changes", "Changes tab"),
                        action("diffChange", "GET", "/api/sessions/{id}/changes/diff", "Diff one path"),
                        action("acceptChange", "POST", "/api/sessions/{id}/changes/accept", "Keep"),
                        action("rejectChange", "POST", "/api/sessions/{id}/changes/reject", "Restore"),
                        action("listEvents", "GET", "/api/sessions/{id}/events", "History"),
                        action("listCollaborators", "GET", "/api/sessions/{id}/collaborators", "Share list"),
                        action("addCollaborator", "POST", "/api/sessions/{id}/collaborators", "Share"),
                        action("removeCollaborator", "DELETE", "/api/sessions/{id}/collaborators/{username}", "Unshare"),
                        action("getSessionGuidance", "GET", "/api/sessions/{id}/guidance", "Guidance tab"),
                        action("putSessionGuidance", "PUT", "/api/sessions/{id}/guidance", "Update guidance"),
                        action("listGuidancePacks", "GET", "/api/guidance/packs", "Rules & Skills library"),
                        action("createGuidancePack", "POST", "/api/guidance/packs", "Create pack"),
                        action("updateGuidancePack", "PATCH", "/api/guidance/packs/{id}", "Update pack"),
                        action("deleteGuidancePack", "DELETE", "/api/guidance/packs/{id}", "Delete pack"),
                        action("getGuidanceDefaults", "GET", "/api/guidance/defaults", "Default packs"),
                        action("putGuidanceDefaults", "PUT", "/api/guidance/defaults", "Set defaults"),
                        action("listGuidanceTemplates", "GET", "/api/guidance/templates", "Templates"),
                        action("installGuidanceTemplates", "POST", "/api/guidance/templates/install", "Install templates"),
                        action("listAudit", "GET", "/api/audit", "Activity / audit"),
                        action("listPorts", "GET", "/api/platform/ports", "Port registry"),
                        action("claimPort", "POST", "/api/platform/ports/claim", "Claim a TCP port"),
                        action("releasePort", "POST", "/api/platform/ports/{port}/release", "Release port"),
                        action("listApps", "GET", "/api/platform/apps", "CSS App Home tiles"),
                        action("listTasks", "GET", "/api/platform/tasks", "Engineering Manager tasks"),
                        action("createTask", "POST", "/api/platform/tasks", "Create Engineering Manager task"),
                        action("getTask", "GET", "/api/platform/tasks/{id}", "Open Engineering Manager task"),
                        action("updateTask", "PATCH", "/api/platform/tasks/{id}", "Update Engineering Manager task"),
                        action("listRoles", "GET", "/api/platform/roles", "Engineering role catalog"),
                        action("getRole", "GET", "/api/platform/roles/{id}", "Role ACL and prompt hint"),
                        action("orgDashboard", "GET", "/api/platform/org", "VirtualDev Co org dashboard"),
                        action("swarmTick", "POST", "/api/platform/swarm/tick", "Advance pipeline swarm handoffs"),
                        action("listMemory", "GET", "/api/platform/memory", "Shared project memory"),
                        action("upsertMemory", "POST", "/api/platform/memory", "Upsert shared memory entry"),
                        action("listMessages", "GET", "/api/platform/messages", "Inter-agent messages"),
                        action("createMessage", "POST", "/api/platform/messages", "Send inter-agent message"),
                        action("listPipelines", "GET", "/api/platform/pipelines", "Workflow pipeline presets"),
                        action("runPipeline", "POST", "/api/platform/pipelines/{id}/run", "Spawn pipeline task graph"),
                        action("linkTaskSession", "POST", "/api/platform/tasks/{id}/session", "Link task to portal session"),
                        action("platformHome", "GET", "/api/platform/home", "App Home payload")
                )
        );
    }

    private static Map<String, String> action(String id, String method, String path, String ui) {
        return Map.of(
                "id", id,
                "method", method,
                "path", path,
                "ui", ui
        );
    }
}
