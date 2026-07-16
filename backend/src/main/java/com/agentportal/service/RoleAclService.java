package com.agentportal.service;

import com.agentportal.dto.PlatformRoleDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Maps Cursor/ACP tool permission payloads onto VirtualDev role ACL categories
 * and decides allow / deny / require-human.
 */
@Service
public class RoleAclService {

    private final PlatformRegistryService platformRegistryService;
    private final ObjectMapper objectMapper;

    public RoleAclService(PlatformRegistryService platformRegistryService, ObjectMapper objectMapper) {
        this.platformRegistryService = platformRegistryService;
        this.objectMapper = objectMapper;
    }

    public Optional<PlatformRoleDto> findRole(String roleId) {
        if (roleId == null || roleId.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(platformRegistryService.getRole(roleId));
        } catch (ResponseStatusException e) {
            return Optional.empty();
        }
    }

    public PlatformRoleDto requireRole(String roleId) {
        return findRole(roleId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown platform role: " + roleId));
    }

    public String normalizeRole(String roleId) {
        return requireRole(roleId).id();
    }

    /**
     * Classify a permission/tool payload into an ACL category such as read, edit, shell.
     */
    public String classifyTool(JsonNode params) {
        if (params == null || params.isNull()) {
            return "read";
        }
        String blob = String.join(" ",
                text(params, "toolName"),
                text(params, "title"),
                text(params, "kind"),
                text(params, "name"),
                text(params.path("toolCall"), "title"),
                text(params.path("toolCall"), "kind"),
                text(params.path("toolCall"), "toolName"),
                text(params.path("toolCall"), "name"),
                params.toString()
        ).toLowerCase(Locale.ROOT);

        // Prefer explicit tool identity before content keywords (prevents shell→read misclass)
        if (containsAny(blob,
                "shell", "bash", "powershell", "cmd.exe", "terminal", "exec",
                "run_terminal", "run_command", "runterminal", "shelltool",
                "\"name\":\"shell\"", "\"toolname\":\"shell\"", "kind\":\"shell")) {
            return "shell";
        }
        if (containsAny(blob, "deploy", "kubectl", "docker push", "helm ")) {
            return "deploy";
        }
        if (containsAny(blob, "port", "nginx", "cloudflare", "dns")) {
            return "ports";
        }
        if (containsAny(blob, "playwright", "cypress", "pytest", "junit", "test ")) {
            return "test";
        }
        if (containsAny(blob, "browser", "webfetch", "websearch", "http get", "fetch(")) {
            return "browser";
        }
        if (containsAny(blob, "audit", "security scan", "secret scan")) {
            return "audit";
        }
        if (containsAny(blob, "write", "strreplace", "editnotebook", "delete", "apply_patch", "create file", "edit ")) {
            return "edit";
        }
        if (containsAny(blob, "grep", "glob", "search", "semsearch")) {
            return "search";
        }
        if (containsAny(blob, "readme", "docs", "markdown")) {
            return "docs";
        }
        if (containsAny(blob, "read", "cat ", "open file", "list_dir", "listdir")) {
            return "read";
        }
        return "read";
    }

    public String classifyToolFromDetailsJson(String detailsJson) {
        if (detailsJson == null || detailsJson.isBlank()) {
            return "read";
        }
        try {
            return classifyTool(objectMapper.readTree(detailsJson));
        } catch (Exception e) {
            String lower = detailsJson.toLowerCase(Locale.ROOT);
            if (lower.contains("shell") || lower.contains("bash")) {
                return "shell";
            }
            if (lower.contains("write") || lower.contains("edit")) {
                return "edit";
            }
            return "read";
        }
    }

    public boolean isToolAllowed(PlatformRoleDto role, String category) {
        if (role == null || category == null || category.isBlank()) {
            return true;
        }
        Set<String> allowed = Set.copyOf(role.allowedTools());
        String cat = category.toLowerCase(Locale.ROOT);
        if (allowed.contains(cat)) {
            return true;
        }
        // read implies search/docs for convenience
        if (("search".equals(cat) || "docs".equals(cat)) && allowed.contains("read")) {
            return true;
        }
        return false;
    }

    public boolean isActionAllowed(PlatformRoleDto role, String actionId) {
        if (role == null || actionId == null || actionId.isBlank()) {
            return true;
        }
        return role.allowedActions().stream().anyMatch(a -> a.equalsIgnoreCase(actionId));
    }

    public void assertSessionAction(String platformRole, String actionId) {
        if (platformRole == null || platformRole.isBlank()) {
            return;
        }
        PlatformRoleDto role = requireRole(platformRole);
        if (!isActionAllowed(role, actionId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Role " + role.id() + " cannot perform action " + actionId
                            + ". Allowed: " + String.join(", ", role.allowedActions()));
        }
    }

    public String buildRolePromptPrefix(PlatformRoleDto role) {
        if (role == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[VirtualDev role: ").append(role.id()).append(" — ").append(role.name()).append("]\n");
        sb.append(role.promptHint()).append("\n");
        sb.append("Allowed tools: ").append(String.join(", ", role.allowedTools())).append(".\n");
        sb.append("Allowed portal actions: ").append(String.join(", ", role.allowedActions())).append(".\n");
        if (role.humanApprovalRequired()) {
            sb.append("Human approval is required for tool permissions in this role.\n");
        }
        sb.append("Do not use tools outside the allowed list.\n\n");
        return sb.toString();
    }

    private static boolean containsAny(String blob, String... needles) {
        for (String n : needles) {
            if (blob.contains(n)) {
                return true;
            }
        }
        return false;
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? "" : v.asText("");
    }
}
