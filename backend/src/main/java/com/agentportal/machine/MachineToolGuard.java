package com.agentportal.machine;

import com.agentportal.config.AppProperties;
import com.agentportal.service.WorkspacePathResolver;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Hard intercepts for GATEWAY_* sessions: path allowlist + shell kill allowlist shapes.
 */
@Component
public class MachineToolGuard {

    private static final Pattern STOP_PROCESS_ID = Pattern.compile(
            "(?is)^\\s*Stop-Process\\s+-Id\\s+\\d+(?:\\s+-Force)?\\s*$"
    );
    /** Allowlist: taskkill /PID <digits> [/F] */
    private static final Pattern TASKKILL_PID = Pattern.compile(
            "(?is)^\\s*taskkill\\s+(/PID\\s+)\\d+(\\s+/F)?\\s*$"
    );
    /** Allowlist: Get-NetTCPConnection ... -LocalPort <digits> ... */
    private static final Pattern NET_TCP_PORT = Pattern.compile(
            "(?is).*Get-NetTCPConnection.*-LocalPort\\s+\\d+.*"
    );

    private final AppProperties appProperties;
    private final WorkspacePathResolver workspacePathResolver;

    public MachineToolGuard(AppProperties appProperties, WorkspacePathResolver workspacePathResolver) {
        this.appProperties = appProperties;
        this.workspacePathResolver = workspacePathResolver;
    }

    public boolean isGatewayRole(String platformRole) {
        return platformRole != null && platformRole.toUpperCase(Locale.ROOT).startsWith("GATEWAY_");
    }

    /**
     * Reject filesystem-mutating tools whose paths escape workspace ∪ sandbox.
     */
    public void assertEditPaths(String sessionWorkspacePath, JsonNode params) {
        List<String> paths = extractPaths(params);
        if (paths.isEmpty()) {
            // No path found — deny mutating tool rather than guess
            throw new SecurityException("GATEWAY path guard: mutating tool missing resolvable path");
        }
        Path workspace = Path.of(sessionWorkspacePath).toAbsolutePath().normalize();
        Path sandbox = resolveSandboxRoot();
        for (String raw : paths) {
            Path candidate = resolveCandidate(raw, workspace);
            if (!isUnderAllowed(candidate, workspace, sandbox)) {
                throw new SecurityException(
                        "GATEWAY path guard: write outside workspace/sandbox rejected: " + candidate);
            }
        }
    }

    /**
     * For GATEWAY sessions: shell commands must match allowlisted shapes only
     * (port→PID lookup or PID termination). All other shell is rejected.
     */
    public void assertShellSafe(JsonNode params) {
        String command = extractCommand(params);
        if (command == null || command.isBlank()) {
            throw new SecurityException("GATEWAY shell guard: empty shell command rejected");
        }
        String normalized = command.replace('"', ' ').replace('\'', ' ').trim();
        if (STOP_PROCESS_ID.matcher(normalized).matches()
                || TASKKILL_PID.matcher(normalized).matches()
                || NET_TCP_PORT.matcher(normalized).matches()) {
            return;
        }
        throw new SecurityException(
                "GATEWAY shell guard: only allowlisted port→PID / -Id shapes are permitted "
                        + "(Stop-Process -Id <pid>, taskkill /PID <pid>, Get-NetTCPConnection -LocalPort).");
    }

    public String denyReasonOrNull(String platformRole, String category, String sessionWorkspacePath, JsonNode params) {
        if (!isGatewayRole(platformRole)) {
            return null;
        }
        String cat = category == null ? "" : category.toLowerCase(Locale.ROOT);
        try {
            if (isFilesystemMutating(cat, params)) {
                assertEditPaths(sessionWorkspacePath, params);
            }
            if ("shell".equals(cat)) {
                assertShellSafe(params);
            }
            return null;
        } catch (SecurityException ex) {
            return ex.getMessage();
        }
    }

    static boolean isFilesystemMutating(String category, JsonNode params) {
        if ("edit".equals(category) || "deploy".equals(category)) {
            return true;
        }
        String blob = params == null ? "" : params.toString().toLowerCase(Locale.ROOT);
        return blob.contains("delete")
                || blob.contains("move")
                || blob.contains("rename")
                || blob.contains("write")
                || blob.contains("strreplace")
                || blob.contains("apply_patch")
                || blob.contains("create file");
    }

    private Path resolveSandboxRoot() {
        String configured = appProperties.getMachineGateway().getSandboxRoot();
        if (configured == null || configured.isBlank()) {
            return null;
        }
        return Path.of(configured.trim()).toAbsolutePath().normalize();
    }

    private Path resolveCandidate(String raw, Path workspace) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("file:")) {
            trimmed = trimmed.substring(5);
            if (trimmed.startsWith("///")) {
                trimmed = trimmed.substring(2);
            } else if (trimmed.startsWith("//")) {
                trimmed = trimmed.substring(1);
            }
        }
        if (trimmed.contains("..")) {
            throw new SecurityException("GATEWAY path guard: '..' not allowed");
        }
        Path p = Path.of(trimmed);
        if (!p.isAbsolute()) {
            p = workspace.resolve(trimmed);
        }
        p = p.toAbsolutePath().normalize();
        try {
            if (java.nio.file.Files.exists(p)) {
                p = p.toRealPath();
            }
        } catch (Exception e) {
            throw new SecurityException("GATEWAY path guard: cannot resolve real path: " + p);
        }
        return p;
    }

    private boolean isUnderAllowed(Path candidate, Path workspace, Path sandbox) {
        Path ws = realIfExists(workspace);
        if (WorkspacePathResolver.isUnder(candidate, ws)) {
            return true;
        }
        if (sandbox != null) {
            Path sb = realIfExists(sandbox);
            if (WorkspacePathResolver.isUnder(candidate, sb)) {
                return true;
            }
        }
        return workspacePathResolver.isAllowed(candidate);
    }

    private static Path realIfExists(Path p) {
        try {
            if (java.nio.file.Files.exists(p)) {
                return p.toRealPath();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return p.toAbsolutePath().normalize();
    }

    static List<String> extractPaths(JsonNode params) {
        List<String> out = new ArrayList<>();
        collectPaths(params, out);
        return out;
    }

    private static void collectPaths(JsonNode node, List<String> out) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(e -> {
                String key = e.getKey().toLowerCase(Locale.ROOT);
                JsonNode v = e.getValue();
                if (v != null && v.isTextual() && isPathKey(key)) {
                    out.add(v.asText());
                } else {
                    collectPaths(v, out);
                }
            });
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                collectPaths(child, out);
            }
        }
    }

    private static boolean isPathKey(String key) {
        return key.equals("path")
                || key.equals("filepath")
                || key.equals("file_path")
                || key.equals("file")
                || key.equals("dest")
                || key.equals("src")
                || key.equals("destfile")
                || key.equals("source")
                || key.equals("target")
                || key.equals("targetpath")
                || key.equals("uri")
                || key.equals("filename")
                || key.equals("directory")
                || key.equals("dir")
                || key.equals("workdir")
                || key.equals("cwd")
                || key.equals("workspace")
                || key.equals("location")
                || key.equals("output")
                || key.equals("outdir")
                || key.endsWith("path");
    }

    static String extractCommand(JsonNode params) {
        if (params == null || params.isNull()) {
            return "";
        }
        for (String key : List.of("command", "cmd", "script", "input", "code")) {
            JsonNode n = params.get(key);
            if (n != null && n.isTextual() && !n.asText().isBlank()) {
                return n.asText();
            }
        }
        JsonNode toolCall = params.path("toolCall");
        if (!toolCall.isMissingNode()) {
            for (String key : List.of("command", "cmd", "script", "input")) {
                JsonNode n = toolCall.get(key);
                if (n != null && n.isTextual() && !n.asText().isBlank()) {
                    return n.asText();
                }
            }
        }
        // Fallback: whole blob for pattern matching
        return params.toString();
    }
}
