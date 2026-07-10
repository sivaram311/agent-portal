package com.agentportal.service;

import com.agentportal.dto.FileContentDto;
import com.agentportal.dto.FileEntryDto;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class WorkspaceFileService {

    private static final long MAX_BYTES = 512 * 1024;

    public List<FileEntryDto> list(Path workspaceRoot, String relativePath) throws IOException {
        Path root = realRoot(workspaceRoot);
        Path dir = resolveSafe(root, relativePath);
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Not a directory: " + relativePath);
        }
        List<FileEntryDto> out = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                String name = p.getFileName().toString();
                if (name.startsWith(".agent-portal")) {
                    continue;
                }
                boolean isDir = Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS);
                if (Files.isSymbolicLink(p)) {
                    continue;
                }
                long size = isDir ? 0 : Files.size(p);
                String rel = root.relativize(p.toAbsolutePath().normalize()).toString().replace('\\', '/');
                out.add(new FileEntryDto(name, rel, isDir, size));
            }
        }
        out.sort(Comparator
                .comparing(FileEntryDto::directory).reversed()
                .thenComparing(e -> e.name().toLowerCase(Locale.ROOT)));
        return out;
    }

    public FileContentDto read(Path workspaceRoot, String relativePath) throws IOException {
        Path root = realRoot(workspaceRoot);
        Path file = resolveSafe(root, relativePath);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(file)) {
            throw new IllegalArgumentException("Not a file: " + relativePath);
        }
        long size = Files.size(file);
        boolean truncated = size > MAX_BYTES;
        byte[] bytes = Files.readAllBytes(file);
        if (truncated) {
            byte[] slice = new byte[(int) MAX_BYTES];
            System.arraycopy(bytes, 0, slice, 0, slice.length);
            bytes = slice;
        }
        String media = probeMedia(file);
        String content;
        if (media.startsWith("text/") || media.contains("json") || media.contains("xml")
                || media.contains("javascript") || media.contains("yaml") || isLikelyText(file)) {
            content = new String(bytes, StandardCharsets.UTF_8);
        } else if (media.startsWith("image/")) {
            content = "data:" + media + ";base64," + java.util.Base64.getEncoder().encodeToString(bytes);
        } else {
            throw new IllegalArgumentException("Unsupported file type for preview: " + media);
        }
        String rel = root.relativize(file).toString().replace('\\', '/');
        return new FileContentDto(rel, content, media, truncated);
    }

    private Path realRoot(Path workspaceRoot) throws IOException {
        Path root = workspaceRoot.toAbsolutePath().normalize();
        if (Files.exists(root)) {
            try {
                return root.toRealPath();
            } catch (IOException ignored) {
                return root;
            }
        }
        return root;
    }

    private Path resolveSafe(Path root, String relativePath) throws IOException {
        String rel = relativePath == null || relativePath.isBlank() ? "." : relativePath;
        if (rel.contains("..")) {
            throw new SecurityException("Path escapes workspace");
        }
        Path resolved = root.resolve(rel).toAbsolutePath().normalize();
        if (!resolved.startsWith(root)) {
            throw new SecurityException("Path escapes workspace");
        }
        if (Files.exists(resolved)) {
            Path real = resolved.toRealPath();
            if (!real.startsWith(root)) {
                throw new SecurityException("Path escapes workspace via link");
            }
            return real;
        }
        return resolved;
    }

    private String probeMedia(Path file) throws IOException {
        String probed = Files.probeContentType(file);
        if (probed != null) {
            return probed;
        }
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".md")) return "text/markdown";
        if (name.endsWith(".java")) return "text/x-java-source";
        if (name.endsWith(".ts") || name.endsWith(".tsx")) return "text/typescript";
        if (name.endsWith(".js")) return "text/javascript";
        if (name.endsWith(".json")) return "application/json";
        if (name.endsWith(".css") || name.endsWith(".scss")) return "text/css";
        if (name.endsWith(".html")) return "text/html";
        if (name.endsWith(".xml")) return "application/xml";
        if (name.endsWith(".yml") || name.endsWith(".yaml")) return "text/yaml";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    private boolean isLikelyText(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".md") || name.endsWith(".txt") || name.endsWith(".java")
                || name.endsWith(".ts") || name.endsWith(".tsx") || name.endsWith(".js")
                || name.endsWith(".json") || name.endsWith(".xml") || name.endsWith(".yml")
                || name.endsWith(".yaml") || name.endsWith(".properties") || name.endsWith(".gradle")
                || name.endsWith(".sql") || name.endsWith(".sh") || name.endsWith(".ps1")
                || name.endsWith(".css") || name.endsWith(".scss") || name.endsWith(".html");
    }
}
