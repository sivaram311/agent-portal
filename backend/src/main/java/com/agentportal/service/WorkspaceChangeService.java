package com.agentportal.service;

import com.agentportal.dto.FileChangeDto;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Service
public class WorkspaceChangeService {

    private static final int MAX_DIFF_CHARS = 256 * 1024;
    private static final int MAX_FILES = 500;

    private final Map<UUID, Map<String, FileFingerprint>> baselines = new ConcurrentHashMap<>();

    public void captureBaseline(UUID sessionId, Path workspaceRoot) throws IOException {
        baselines.put(sessionId, scan(workspaceRoot));
    }

    public List<FileChangeDto> listChanges(UUID sessionId, Path workspaceRoot) throws IOException {
        List<FileChangeDto> gitChanges = tryGitStatus(workspaceRoot);
        if (!gitChanges.isEmpty()) {
            return gitChanges;
        }
        Map<String, FileFingerprint> before = baselines.getOrDefault(sessionId, Map.of());
        Map<String, FileFingerprint> after = scan(workspaceRoot);
        List<FileChangeDto> changes = new ArrayList<>();

        Set<String> paths = new TreeSet<>();
        paths.addAll(before.keySet());
        paths.addAll(after.keySet());

        for (String rel : paths) {
            FileFingerprint b = before.get(rel);
            FileFingerprint a = after.get(rel);
            if (b == null && a != null) {
                changes.add(new FileChangeDto(rel, "added", a.size(), null, "snapshot"));
            } else if (b != null && a == null) {
                changes.add(new FileChangeDto(rel, "deleted", b.size(), null, "snapshot"));
            } else if (b != null && a != null && !Objects.equals(b.digest(), a.digest())) {
                changes.add(new FileChangeDto(rel, "modified", a.size(), null, "snapshot"));
            }
        }
        return changes;
    }

    public FileChangeDto diffFile(UUID sessionId, Path workspaceRoot, String relativePath) throws IOException {
        Path root = workspaceRoot.toAbsolutePath().normalize();
        String rel = relativePath.replace('\\', '/');
        if (rel.contains("..")) {
            throw new SecurityException("Path escapes workspace");
        }
        Path file = root.resolve(rel).normalize();
        if (!file.startsWith(root)) {
            throw new SecurityException("Path escapes workspace");
        }
        Map<String, FileFingerprint> before = baselines.getOrDefault(sessionId, Map.of());
        FileFingerprint b = before.get(rel);
        String oldText = b == null ? "" : b.preview();
        String newText = "";
        String status = "deleted";
        long size = 0;
        if (Files.isRegularFile(file)) {
            byte[] bytes = Files.readAllBytes(file);
            size = bytes.length;
            newText = isText(file) ? truncate(new String(bytes, StandardCharsets.UTF_8)) : "";
            status = b == null ? "added" : "modified";
        }
        String unified = buildUnified(rel, oldText, newText);
        return new FileChangeDto(rel, status, size, unified, baselines.containsKey(sessionId) ? "snapshot" : "live");
    }

    private Map<String, FileFingerprint> scan(Path workspaceRoot) throws IOException {
        Path root = workspaceRoot.toAbsolutePath().normalize();
        Map<String, FileFingerprint> map = new HashMap<>();
        if (!Files.isDirectory(root)) {
            return map;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS))
                    .filter(p -> !Files.isSymbolicLink(p))
                    .limit(MAX_FILES)
                    .forEach(p -> {
                        try {
                            String rel = root.relativize(p).toString().replace('\\', '/');
                            if (rel.startsWith(".agent-portal") || rel.startsWith(".git/")) {
                                return;
                            }
                            BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                            byte[] bytes = Files.readAllBytes(p);
                            String digest = sha256(bytes);
                            String preview = isText(p) ? truncate(new String(bytes, StandardCharsets.UTF_8)) : "";
                            map.put(rel, new FileFingerprint(attrs.size(), digest, preview));
                        } catch (Exception ignored) {
                        }
                    });
        }
        return map;
    }

    private List<FileChangeDto> tryGitStatus(Path workspaceRoot) {
        try {
            Path root = workspaceRoot.toAbsolutePath().normalize();
            if (!Files.isDirectory(root.resolve(".git"))) {
                return List.of();
            }
            ProcessBuilder pb = new ProcessBuilder("git", "status", "--porcelain", "-uall");
            pb.directory(root.toFile());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return List.of();
            }
            List<FileChangeDto> list = new ArrayList<>();
            for (String line : out.split("\\R")) {
                if (line.length() < 4) {
                    continue;
                }
                String code = line.substring(0, 2).trim();
                String path = line.substring(3).trim().replace('\\', '/');
                if (path.contains(" -> ")) {
                    path = path.substring(path.lastIndexOf(" -> ") + 4);
                }
                String status = switch (code) {
                    case "A", "??" -> "added";
                    case "D" -> "deleted";
                    default -> "modified";
                };
                long size = 0;
                Path f = root.resolve(path);
                if (Files.isRegularFile(f)) {
                    size = Files.size(f);
                }
                list.add(new FileChangeDto(path, status, size, null, "git"));
            }
            return list;
        } catch (Exception e) {
            return List.of();
        }
    }

    private String buildUnified(String path, String oldText, String newText) {
        if (Objects.equals(oldText, newText)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("--- a/").append(path).append('\n');
        sb.append("+++ b/").append(path).append('\n');
        for (String line : oldText.split("\\R", -1)) {
            sb.append('-').append(line).append('\n');
        }
        for (String line : newText.split("\\R", -1)) {
            sb.append('+').append(line).append('\n');
        }
        String u = sb.toString();
        return u.length() > MAX_DIFF_CHARS ? u.substring(0, MAX_DIFF_CHARS) + "\n… truncated …\n" : u;
    }

    private boolean isText(Path file) {
        String n = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return n.endsWith(".md") || n.endsWith(".txt") || n.endsWith(".java") || n.endsWith(".ts")
                || n.endsWith(".tsx") || n.endsWith(".js") || n.endsWith(".json") || n.endsWith(".xml")
                || n.endsWith(".yml") || n.endsWith(".yaml") || n.endsWith(".properties") || n.endsWith(".css")
                || n.endsWith(".scss") || n.endsWith(".html") || n.endsWith(".sql") || n.endsWith(".sh")
                || n.endsWith(".ps1") || n.endsWith(".py") || n.endsWith(".gradle");
    }

    private String truncate(String s) {
        return s.length() > MAX_DIFF_CHARS ? s.substring(0, MAX_DIFF_CHARS) : s;
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : dig) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(Arrays.hashCode(bytes));
        }
    }

    private record FileFingerprint(long size, String digest, String preview) {
    }
}
