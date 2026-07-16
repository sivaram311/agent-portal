package com.agentportal.service;

import com.agentportal.config.AgentProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Resolves session/platform {@code workspacePath} values under
 * {@code agent.workspace.root}, or under an explicitly configured allowlisted root.
 */
@Component
public class WorkspacePathResolver {

    private final AgentProperties agentProperties;

    public WorkspacePathResolver(AgentProperties agentProperties) {
        this.agentProperties = agentProperties;
    }

    public Path root() {
        return Path.of(agentProperties.getWorkspace().getRoot()).toAbsolutePath().normalize();
    }

    public List<Path> allowedRoots() {
        List<Path> roots = new ArrayList<>();
        roots.add(root());
        for (String entry : agentProperties.getWorkspace().getAllowedRoots()) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            roots.add(Path.of(entry.trim()).toAbsolutePath().normalize());
        }
        return roots;
    }

    /**
     * @throws IllegalArgumentException if blank
     * @throws SecurityException if path escapes root and allowlist
     */
    public Path resolve(String requested) {
        if (requested == null || requested.isBlank()) {
            throw new IllegalArgumentException("workspacePath is required");
        }
        String trimmed = requested.trim();
        if (trimmed.contains("..")) {
            throw new SecurityException("workspacePath must not contain '..'");
        }

        Path candidate;
        if (isAbsoluteRequest(trimmed)) {
            candidate = Path.of(trimmed).toAbsolutePath().normalize();
        } else {
            candidate = root().resolve(trimmed).toAbsolutePath().normalize();
        }

        if (!isAllowed(candidate)) {
            throw new SecurityException(
                    "workspacePath is not allowed; must stay under " + describeAllowedRoots());
        }
        return candidate;
    }

    public boolean isAllowed(Path candidate) {
        Path normalized = candidate.toAbsolutePath().normalize();
        for (Path base : allowedRoots()) {
            if (isUnder(normalized, base)) {
                return true;
            }
        }
        return false;
    }

    static boolean isAbsoluteRequest(String trimmed) {
        return Path.of(trimmed).isAbsolute()
                || trimmed.contains(":")
                || trimmed.startsWith("/")
                || trimmed.startsWith("\\");
    }

    public static boolean isUnder(Path candidate, Path base) {
        Path c = candidate.toAbsolutePath().normalize();
        Path b = base.toAbsolutePath().normalize();
        if (c.startsWith(b)) {
            return true;
        }
        // Windows: tolerate drive-letter / separator case differences across Path providers
        String cs = c.toString().replace('/', '\\');
        String bs = b.toString().replace('/', '\\');
        if (cs.length() < bs.length()) {
            return false;
        }
        if (!cs.regionMatches(true, 0, bs, 0, bs.length())) {
            return false;
        }
        return cs.length() == bs.length() || cs.charAt(bs.length()) == '\\';
    }

    private String describeAllowedRoots() {
        return allowedRoots().stream()
                .map(p -> p.toString())
                .collect(Collectors.joining(", "));
    }
}
