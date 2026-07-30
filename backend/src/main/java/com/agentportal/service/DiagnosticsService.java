package com.agentportal.service;

import com.agentportal.config.AppProperties;
import com.agentportal.dto.ClientDiagnosticsRequest;
import com.agentportal.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class DiagnosticsService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;

    private final AppProperties appProperties;

    public DiagnosticsService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public Map<String, Object> storeClientLog(ClientDiagnosticsRequest request) {
        String body = request.bodyText();
        long max = Math.max(16_384L, appProperties.getDiagnostics().getMaxBodyBytes());
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > max) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Diagnostics payload exceeds " + max + " bytes");
        }

        String user = sanitize(CurrentUser.usernameOrAnonymous(), 64);
        String device = sanitize(request.deviceId(), 64);
        String reason = sanitize(
                request.reason() == null || request.reason().isBlank() ? "manual" : request.reason(),
                32);
        String platform = sanitize(
                request.platform() == null || request.platform().isBlank() ? "android" : request.platform(),
                32);
        long epoch = Instant.now().getEpochSecond();
        LocalDate day = LocalDate.now(ZoneOffset.UTC);
        String fileName = user + "_" + device + "_" + epoch + "_" + reason + ".log";

        Path root = diagnosticsRoot();
        Path dayDir = root.resolve(DAY.format(day));
        Path target = dayDir.resolve(fileName).normalize();
        if (!target.startsWith(root.normalize())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid diagnostics path");
        }

        String header = """
                # Agent Portal mobile diagnostics
                # user=%s
                # deviceId=%s
                # platform=%s
                # appVersion=%s
                # versionCode=%s
                # reason=%s
                # createdAt=%s
                # receivedAt=%s
                #
                """.formatted(
                user,
                device,
                platform,
                nullToDash(request.appVersion()),
                request.versionCode() == null ? "-" : request.versionCode().toString(),
                reason,
                nullToDash(request.createdAt()),
                Instant.now().toString()
        );

        try {
            Files.createDirectories(dayDir);
            Files.writeString(target, header + body + (body.endsWith("\n") ? "" : "\n"),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to write diagnostics file: " + ex.getMessage());
        }

        String relative = root.relativize(target).toString().replace('\\', '/');
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("path", relative);
        result.put("bytes", Files.exists(target) ? target.toFile().length() : bytes.length);
        return result;
    }

    public List<Map<String, Object>> listClientLogs(int limit) {
        requireAdmin();
        int cap = Math.min(Math.max(limit, 1), 100);
        Path root = diagnosticsRoot();
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root, 2)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".log"))
                    .forEach(files::add);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
        files.sort(Comparator.comparingLong((Path p) -> p.toFile().lastModified()).reversed());
        List<Map<String, Object>> out = new ArrayList<>();
        for (Path p : files.stream().limit(cap).toList()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("path", root.relativize(p).toString().replace('\\', '/'));
            row.put("bytes", p.toFile().length());
            row.put("mtime", Instant.ofEpochMilli(p.toFile().lastModified()).toString());
            out.add(row);
        }
        return out;
    }

    public byte[] readClientLog(String relativePath) {
        requireAdmin();
        Path root = diagnosticsRoot().normalize();
        String cleaned = relativePath == null ? "" : relativePath.replace('\\', '/');
        if (cleaned.contains("..") || cleaned.startsWith("/") || cleaned.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid path");
        }
        Path target = root.resolve(cleaned).normalize();
        if (!target.startsWith(root) || !Files.isRegularFile(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Diagnostics file not found");
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    private Path diagnosticsRoot() {
        String dir = appProperties.getDiagnostics().getDir();
        if (dir == null || dir.isBlank()) {
            dir = "./logs/mobile-diagnostics";
        }
        return Path.of(dir).toAbsolutePath().normalize();
    }

    private static void requireAdmin() {
        if (!CurrentUser.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only");
        }
    }

    private static String sanitize(String raw, int max) {
        String s = raw == null ? "unknown" : raw.trim().toLowerCase(Locale.ROOT);
        s = s.replaceAll("[^a-z0-9._-]+", "_");
        if (s.isBlank()) {
            s = "unknown";
        }
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
