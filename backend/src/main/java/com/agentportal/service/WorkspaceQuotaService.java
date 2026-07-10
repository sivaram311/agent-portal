package com.agentportal.service;

import com.agentportal.config.AgentProperties;
import com.agentportal.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class WorkspaceQuotaService {

    private final AgentProperties agentProperties;

    public WorkspaceQuotaService(AgentProperties agentProperties) {
        this.agentProperties = agentProperties;
    }

    public void assertWithinQuota(Path workspaceRoot) throws IOException {
        long limit = agentProperties.getWorkspace().getQuotaBytesPerUser();
        if (limit <= 0) {
            return;
        }
        Path userRoot = Path.of(agentProperties.getWorkspace().getRoot()).toAbsolutePath().normalize()
                .resolve(sanitize(CurrentUser.usernameOrAnonymous()));
        // Quota is measured under workspace root total for simplicity when user folders aren't used.
        Path measureRoot = Files.isDirectory(userRoot) ? userRoot
                : Path.of(agentProperties.getWorkspace().getRoot()).toAbsolutePath().normalize();
        long used = directorySize(measureRoot);
        long upcoming = Files.exists(workspaceRoot) ? directorySize(workspaceRoot.toAbsolutePath().normalize()) : 0;
        // If workspace is already under measureRoot, don't double-count heavily — use max of used.
        long total = Math.max(used, upcoming);
        if (total > limit) {
            throw new IllegalStateException("Workspace quota exceeded (" + total + " > " + limit + " bytes)");
        }
    }

    private String sanitize(String username) {
        return username.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private long directorySize(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return 0;
        }
        AtomicLong size = new AtomicLong();
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                size.addAndGet(attrs.size());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
        return size.get();
    }
}
