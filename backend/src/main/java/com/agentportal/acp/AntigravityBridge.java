package com.agentportal.acp;

import com.agentportal.config.AgentProperties;
import com.agentportal.domain.*;
import com.agentportal.dto.AgentEventDto;
import com.agentportal.repo.*;
import com.agentportal.service.SessionEventBus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * Antigravity ({@code agy}) runtime: spawn print-mode process and stream events
 * by watching brain conversation artifacts (messages/, tasks/, transcript*.jsonl).
 */
public class AntigravityBridge implements SessionAgentRuntime {

    private static final Logger log = LoggerFactory.getLogger(AntigravityBridge.class);

    private final UUID portalSessionId;
    private final String workspacePath;
    private final AgentProperties properties;
    private final ObjectMapper mapper;
    private final SessionEventBus eventBus;
    private final AgentSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final AgentEventRepository eventRepository;
    private final ToolRunRepository toolRunRepository;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<Process> activeProcess = new AtomicReference<>();
    private final Set<String> seenMessageFiles = ConcurrentHashMap.newKeySet();
    private final Set<String> seenTaskFiles = ConcurrentHashMap.newKeySet();
    private final StringBuilder assistantBuffer = new StringBuilder();
    private final AtomicReference<UUID> streamingAssistantMessageId = new AtomicReference<>();
    private long transcriptByteOffset = 0;
    private long logByteOffset = 0;
    private Path runLogFile;
    private String conversationId;
    private ScheduledExecutorService watcher;

    public AntigravityBridge(
            UUID portalSessionId,
            String workspacePath,
            String existingConversationId,
            AgentProperties properties,
            ObjectMapper mapper,
            SessionEventBus eventBus,
            AgentSessionRepository sessionRepository,
            ChatMessageRepository messageRepository,
            AgentEventRepository eventRepository,
            ToolRunRepository toolRunRepository
    ) {
        this.portalSessionId = portalSessionId;
        this.workspacePath = workspacePath;
        this.conversationId = existingConversationId;
        this.properties = properties;
        this.mapper = mapper;
        this.eventBus = eventBus;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.eventRepository = eventRepository;
        this.toolRunRepository = toolRunRepository;
    }

    @Override
    public synchronized void ensureStarted() throws Exception {
        Path cwd = Path.of(workspacePath).toAbsolutePath().normalize();
        Files.createDirectories(cwd);
        // Do NOT invent a conversation id from cache here — only resume if this
        // portal session already stored one from a previous successful run.
        emit("bridge_ready", Map.of(
                "provider", "antigravity",
                "conversationId", Objects.toString(conversationId, ""),
                "workspacePath", cwd.toString()
        ));
    }

    @Override
    public synchronized CompletableFuture<Void> prompt(String text) {
        if (running.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Antigravity run already in progress"));
        }
        running.set(true);
        setStatus(SessionStatus.STREAMING);
        assistantBuffer.setLength(0);
        streamingAssistantMessageId.set(null);
        transcriptByteOffset = 0;
        logByteOffset = 0;
        replyFileOffset = 0;
        stdoutFileOffset = 0;

        CompletableFuture<Void> future = new CompletableFuture<>();
        Thread worker = new Thread(() -> runPrompt(text, future), "agy-" + portalSessionId);
        worker.setDaemon(true);
        worker.start();
        return future;
    }

    private void runPrompt(String text, CompletableFuture<Void> future) {
        Path cwd = Path.of(workspacePath).toAbsolutePath().normalize();
        Instant startedAt = Instant.now();
        Path stdoutFile = null;
        Path replyFile = null;
        try {
            Files.createDirectories(cwd);
            runLogFile = cwd.resolve(".agent-portal-agy-" + portalSessionId + ".log");
            stdoutFile = cwd.resolve(".agent-portal-agy-" + portalSessionId + ".out.txt");
            replyFile = cwd.resolve(".agent-portal-reply.txt");
            Path launcherScript = cwd.resolve(".agent-portal-agy-" + portalSessionId + ".ps1");
            logByteOffset = 0;
            Files.deleteIfExists(runLogFile);
            Files.deleteIfExists(stdoutFile);
            Files.deleteIfExists(replyFile);
            Files.deleteIfExists(launcherScript);

            // Keep the user prompt intact; append a single-line instruction (no raw newlines in argv).
            String augmented = text.replace('\r', ' ').replace('\n', ' ').trim()
                    + " Also write your complete final answer verbatim to .agent-portal-reply.txt in the workspace root.";

            List<String> command = buildCommand(augmented, cwd, runLogFile, launcherScript);
            log.info("Starting agy for session {} in {}: {}", portalSessionId, cwd, String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(cwd.toFile());
            // Inherit parent stdio. Redirecting/piping stdout often hangs agy print-mode on Windows.
            // Portal streaming uses reply file + brain artifacts + --log-file.
            Process process = pb.start();
            activeProcess.set(process);

            startWatcher(cwd, startedAt, replyFile, stdoutFile);

            int exit = process.waitFor();
            stopWatcher();
            activeProcess.compareAndSet(process, null);

            discoverConversation(cwd, startedAt);
            pollRunLog();
            pollBrainArtifacts(startedAt);
            pollReplyFile(replyFile);
            pollStdoutFile(stdoutFile);
            if (assistantBuffer.isEmpty()) {
                recoverLatestAssistantFromMessages(startedAt).ifPresent(this::appendAssistantDelta);
            }
            flushAssistantMessage();

            if (exit == 0) {
                setStatus(SessionStatus.IDLE);
                emit("run_completed", Map.of(
                        "stopReason", "end_turn",
                        "exitCode", exit,
                        "conversationId", Objects.toString(conversationId, "")
                ));
                future.complete(null);
            } else {
                setStatus(SessionStatus.FAILED);
                emit("run_failed", Map.of("error", "agy exited with code " + exit, "exitCode", exit));
                future.completeExceptionally(new RuntimeException("agy exited with code " + exit));
            }
        } catch (Exception e) {
            stopWatcher();
            activeProcess.set(null);
            flushAssistantMessage();
            setStatus(SessionStatus.FAILED);
            emit("run_failed", Map.of("error", String.valueOf(e.getMessage())));
            future.completeExceptionally(e);
        } finally {
            running.set(false);
            cleanupTemp(runLogFile);
            cleanupTemp(stdoutFile);
            cleanupTemp(cwd.resolve(".agent-portal-agy-" + portalSessionId + ".err.txt"));
            cleanupTemp(cwd.resolve(".agent-portal-agy-" + portalSessionId + ".ps1"));
            // keep .agent-portal-reply.txt for user inspection
        }
    }

    private void cleanupTemp(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private List<String> buildCommand(String prompt, Path cwd, Path logFile, Path launcherScript) throws IOException {
        AgentProperties.Antigravity cfg = properties.getAntigravity();
        List<String> agyArgs = new ArrayList<>();
        // Prompt must follow -p/--print immediately; other flags after.
        agyArgs.add("-p");
        agyArgs.add(prompt);
        agyArgs.add("--print-timeout");
        agyArgs.add(cfg.getPrintTimeout());
        agyArgs.add("--log-file");
        agyArgs.add(logFile.toString());
        if (cfg.isSkipPermissions()) {
            agyArgs.add("--dangerously-skip-permissions");
        }
        if (conversationId != null && !conversationId.isBlank()) {
            agyArgs.add("--conversation");
            agyArgs.add(conversationId);
        }
        agyArgs.add("--add-dir");
        agyArgs.add(cwd.toString());

        String agyExe = cfg.getCommand();
        // On Windows, Java ProcessBuilder pipes often hang agy print-mode.
        // Launch via PowerShell call-operator splat (NOT Start-Process -ArgumentList):
        // Start-Process joins args with spaces and drops quoting, so multi-word -p prompts
        // arrive truncated and agy replies "message was cut off / incomplete".
        if (isWindows()) {
            StringBuilder sb = new StringBuilder();
            sb.append("$ErrorActionPreference = 'Stop'\n");
            sb.append("$outFile = ").append(psSingleQuote(cwd.resolve(".agent-portal-agy-" + portalSessionId + ".out.txt").toString())).append("\n");
            sb.append("$errFile = ").append(psSingleQuote(cwd.resolve(".agent-portal-agy-" + portalSessionId + ".err.txt").toString())).append("\n");
            sb.append("if (Test-Path $outFile) { Remove-Item $outFile -Force }\n");
            sb.append("if (Test-Path $errFile) { Remove-Item $errFile -Force }\n");
            sb.append("Set-Location -LiteralPath ").append(psSingleQuote(cwd.toString())).append("\n");
            sb.append("$agyArgs = @(");
            for (int i = 0; i < agyArgs.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(psSingleQuote(agyArgs.get(i)));
            }
            sb.append(")\n");
            // Capture via call-operator (preserves argv). Avoid `>` redirect — Windows
            // PowerShell writes UTF-16 by default and corrupts the portal reply text.
            sb.append("$output = & ").append(psSingleQuote(agyExe)).append(" @agyArgs 2> $errFile\n");
            sb.append("$exitCode = $LASTEXITCODE\n");
            sb.append("if ($null -eq $output) { $text = '' }\n");
            sb.append("elseif ($output -is [System.Array]) { $text = ($output -join [Environment]::NewLine) }\n");
            sb.append("else { $text = [string]$output }\n");
            sb.append("[System.IO.File]::WriteAllText($outFile, $text, [System.Text.UTF8Encoding]::new($false))\n");
            sb.append("exit $exitCode\n");
            Files.writeString(launcherScript, sb.toString(), StandardCharsets.UTF_8);
            return List.of(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy", "Bypass",
                    "-File",
                    launcherScript.toString()
            );
        }

        List<String> command = new ArrayList<>();
        command.add(agyExe);
        command.addAll(agyArgs);
        return command;
    }

    private boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("win");
    }

    private String psSingleQuote(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private void startWatcher(Path cwd, Instant startedAt, Path replyFile, Path stdoutFile) {
        stopWatcher();
        long interval = Math.max(200, properties.getAntigravity().getPollIntervalMs());
        watcher = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "agy-watch-" + portalSessionId);
            t.setDaemon(true);
            return t;
        });
        watcher.scheduleAtFixedRate(() -> {
            try {
                discoverConversation(cwd, startedAt);
                pollRunLog();
                pollBrainArtifacts(startedAt);
                pollReplyFile(replyFile);
                pollStdoutFile(stdoutFile);
            } catch (Exception e) {
                log.debug("agy watcher tick failed: {}", e.getMessage());
            }
        }, 300, interval, TimeUnit.MILLISECONDS);
    }

    private long replyFileOffset = 0;
    private long stdoutFileOffset = 0;

    private void pollReplyFile(Path replyFile) {
        if (replyFile == null || !Files.isRegularFile(replyFile)) {
            return;
        }
        try {
            long size = Files.size(replyFile);
            if (size <= replyFileOffset) {
                return;
            }
            String content = Files.readString(replyFile, StandardCharsets.UTF_8).replace("\u0000", "");
            if (content.length() > replyFileOffset) {
                String delta = content.substring((int) Math.min(replyFileOffset, content.length()));
                replyFileOffset = content.length();
                // Stdout often already has the same final answer; avoid duplicating it.
                String existing = assistantBuffer.toString();
                String trimmed = delta.trim();
                if (!trimmed.isBlank() && !existing.contains(trimmed)) {
                    appendAssistantDelta(delta);
                }
            }
        } catch (Exception e) {
            log.debug("pollReplyFile: {}", e.getMessage());
        }
    }

    private void pollStdoutFile(Path stdoutFile) {
        if (stdoutFile == null || !Files.isRegularFile(stdoutFile)) {
            return;
        }
        try {
            long size = Files.size(stdoutFile);
            if (size <= stdoutFileOffset) {
                return;
            }
            try (var channel = Files.newByteChannel(stdoutFile, StandardOpenOption.READ)) {
                channel.position(stdoutFileOffset);
                byte[] buf = new byte[(int) Math.min(size - stdoutFileOffset, 1024 * 256)];
                int read = channel.read(java.nio.ByteBuffer.wrap(buf));
                if (read <= 0) {
                    return;
                }
                stdoutFileOffset += read;
                String chunk = stripAnsi(new String(buf, 0, read, StandardCharsets.UTF_8));
                // Drop UTF-16 null padding if a launcher ever wrote Unicode by mistake.
                chunk = chunk.replace("\u0000", "");
                if (!chunk.isBlank()) {
                    appendAssistantDelta(chunk);
                    emit("terminal_chunk", Map.of("stream", "stdout", "text", chunk));
                }
            }
        } catch (Exception e) {
            log.debug("pollStdoutFile: {}", e.getMessage());
        }
    }

    private void stopWatcher() {
        if (watcher != null) {
            watcher.shutdownNow();
            watcher = null;
        }
    }

    private void discoverConversation(Path cwd, Instant startedAt) {
        if (conversationId != null && !conversationId.isBlank()) {
            persistConversationId();
            return;
        }

        // Prefer history.jsonl entries created during this run for this workspace.
        lookupConversationIdFromHistory(cwd.toString(), startedAt).ifPresent(id -> {
            conversationId = id;
            persistConversationId();
            emit("conversation_bound", Map.of("conversationId", id));
        });
        if (conversationId != null && !conversationId.isBlank()) {
            return;
        }

        // Newest brain folder modified after this run started (never reuse stale cache).
        Path brain = brainRoot().resolve("brain");
        if (Files.isDirectory(brain)) {
            try (Stream<Path> dirs = Files.list(brain)) {
                dirs.filter(Files::isDirectory)
                        .filter(p -> {
                            try {
                                return Files.getLastModifiedTime(p).toInstant().isAfter(startedAt.minusSeconds(2));
                            } catch (IOException e) {
                                return false;
                            }
                        })
                        .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                        .ifPresent(p -> {
                            conversationId = p.getFileName().toString();
                            persistConversationId();
                            emit("conversation_bound", Map.of("conversationId", conversationId));
                        });
            } catch (IOException ignored) {
            }
        }
    }

    private Optional<String> lookupConversationIdFromHistory(String workspaceAbsolute, Instant startedAt) {
        Path history = brainRoot().resolve("history.jsonl");
        if (!Files.isRegularFile(history)) {
            return Optional.empty();
        }
        String normalized = workspaceAbsolute.replace('/', '\\');
        try {
            List<String> lines = Files.readAllLines(history, StandardCharsets.UTF_8);
            for (int i = lines.size() - 1; i >= 0; i--) {
                String line = lines.get(i);
                if (line.isBlank()) {
                    continue;
                }
                JsonNode node = mapper.readTree(line);
                String ws = node.path("workspace").asText("");
                String id = node.path("conversationId").asText(null);
                long ts = node.path("timestamp").asLong(0);
                Instant when = ts > 1_000_000_000_000L
                        ? Instant.ofEpochMilli(ts)
                        : (ts > 0 ? Instant.ofEpochSecond(ts) : Instant.EPOCH);
                if (id != null && !id.isBlank()
                        && ws.replace('/', '\\').equalsIgnoreCase(normalized)
                        && !when.isBefore(startedAt.minusSeconds(30))) {
                    return Optional.of(id);
                }
            }
        } catch (Exception e) {
            log.debug("history.jsonl parse failed: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private void pollRunLog() {
        if (runLogFile == null || !Files.isRegularFile(runLogFile)) {
            return;
        }
        try {
            long size = Files.size(runLogFile);
            if (size <= logByteOffset) {
                return;
            }
            try (var channel = Files.newByteChannel(runLogFile, StandardOpenOption.READ)) {
                channel.position(logByteOffset);
                byte[] buf = new byte[(int) Math.min(size - logByteOffset, 1024 * 256)];
                int read = channel.read(java.nio.ByteBuffer.wrap(buf));
                if (read <= 0) {
                    return;
                }
                logByteOffset += read;
                String chunk = new String(buf, 0, read, StandardCharsets.UTF_8);
                for (String line : chunk.split("\\R")) {
                    if (line.isBlank()) {
                        continue;
                    }
                    handleCliLogLine(line);
                }
            }
        } catch (Exception e) {
            log.debug("pollRunLog: {}", e.getMessage());
        }
    }

    private void handleCliLogLine(String line) {
        String cleaned = stripAnsi(line);
        // Surface useful progress lines into the terminal panel.
        if (cleaned.contains("Print mode:")
                || cleaned.contains("tool")
                || cleaned.contains("Tool")
                || cleaned.contains("conversation")
                || cleaned.contains("PLANNER")
                || cleaned.contains("authenticated")
                || cleaned.contains("error")
                || cleaned.contains("Error")
                || cleaned.contains("failed")
                || cleaned.contains("Failed")) {
            emit("terminal_chunk", Map.of("stream", "log", "text", cleaned + "\n"));
        }
        // Extract conversation id if logged.
        if ((conversationId == null || conversationId.isBlank()) && cleaned.contains("conversation")) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(?i)conversation(?:ID|Id)?[=: ]+([0-9a-fA-F-]{36})")
                    .matcher(cleaned);
            if (m.find()) {
                conversationId = m.group(1);
                persistConversationId();
                emit("conversation_bound", Map.of("conversationId", conversationId));
            }
        }
    }

    private void persistConversationId() {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        sessionRepository.findById(portalSessionId).ifPresent(s -> {
            if (!conversationId.equals(s.getCursorSessionId())) {
                s.setCursorSessionId(conversationId);
                sessionRepository.save(s);
            }
        });
    }

    private Optional<String> lookupConversationId(String workspaceAbsolute) {
        Path cache = brainRoot().resolve("cache").resolve("last_conversations.json");
        if (!Files.isRegularFile(cache)) {
            return Optional.empty();
        }
        try {
            JsonNode root = mapper.readTree(cache.toFile());
            // Exact match
            if (root.has(workspaceAbsolute)) {
                return Optional.ofNullable(root.path(workspaceAbsolute).asText(null));
            }
            // Case / slash normalized match
            String normalized = workspaceAbsolute.replace('/', '\\');
            Iterator<String> names = root.fieldNames();
            while (names.hasNext()) {
                String key = names.next();
                if (key.replace('/', '\\').equalsIgnoreCase(normalized)) {
                    return Optional.ofNullable(root.path(key).asText(null));
                }
            }
            // Also check history.jsonl for latest matching workspace (any time — used only as soft hint)
            Path history = brainRoot().resolve("history.jsonl");
            if (Files.isRegularFile(history)) {
                List<String> lines = Files.readAllLines(history, StandardCharsets.UTF_8);
                for (int i = lines.size() - 1; i >= 0; i--) {
                    String line = lines.get(i);
                    if (line.isBlank()) {
                        continue;
                    }
                    JsonNode node = mapper.readTree(line);
                    String ws = node.path("workspace").asText("");
                    String id = node.path("conversationId").asText(null);
                    if (id != null && !id.isBlank()
                            && ws.replace('/', '\\').equalsIgnoreCase(normalized)) {
                        return Optional.of(id);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed reading last_conversations.json: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private void pollBrainArtifacts(Instant startedAt) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        Path conv = brainRoot().resolve("brain").resolve(conversationId).resolve(".system_generated");
        pollMessages(conv.resolve("messages"), startedAt);
        pollTasks(conv.resolve("tasks"), startedAt);
        pollTranscript(conv.resolve("logs").resolve("transcript_full.jsonl"));
        pollTranscript(conv.resolve("logs").resolve("transcript.jsonl"));
    }

    private void pollMessages(Path messagesDir, Instant startedAt) {
        if (!Files.isDirectory(messagesDir)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(messagesDir, "*.json")) {
            for (Path file : stream) {
                String key = file.getFileName().toString();
                if (!seenMessageFiles.add(key)) {
                    continue;
                }
                // Skip very old files from prior conversations unless conversation was just bound
                if (Files.getLastModifiedTime(file).toInstant().isBefore(startedAt.minusSeconds(2))) {
                    continue;
                }
                handleMessageFile(file);
            }
        } catch (IOException e) {
            log.debug("pollMessages: {}", e.getMessage());
        }
    }

    private void handleMessageFile(Path file) {
        try {
            JsonNode node = mapper.readTree(file.toFile());
            String title = node.path("renderDetails").path("messageTitle").asText("");
            String content = node.path("content").asText("");
            String sender = node.path("sender").asText("");

            if (title.toLowerCase(Locale.ROOT).contains("finished") || sender.contains("/task-")) {
                String toolName = title.isBlank() ? "task" : title;
                String toolCallId = sender.isBlank() ? file.getFileName().toString() : sender;
                ToolRun run = new ToolRun();
                run.setSessionId(portalSessionId);
                run.setToolCallId(toolCallId);
                run.setToolName(toolName);
                run.setArgsJson("{}");
                run.setStatus("completed");
                run.setOutput(content);
                run.setFinishedAt(Instant.now());
                run = toolRunRepository.save(run);
                emit("tool_call", Map.of(
                        "toolCallId", toolCallId,
                        "toolName", toolName,
                        "status", "completed",
                        "args", "{}",
                        "toolRunId", run.getId().toString()
                ));
                if (content != null && !content.isBlank()) {
                    emit("terminal_chunk", Map.of(
                            "toolCallId", toolCallId,
                            "toolName", toolName,
                            "stream", "stdout",
                            "text", content + "\n"
                    ));
                }
                return;
            }

            // Assistant / agent narrative
            if (content != null && !content.isBlank()) {
                appendAssistantDelta(content);
                if (!content.endsWith("\n")) {
                    appendAssistantDelta("\n");
                }
            }
        } catch (Exception e) {
            log.debug("Failed parsing message {}: {}", file, e.getMessage());
        }
    }

    private void pollTasks(Path tasksDir, Instant startedAt) {
        if (!Files.isDirectory(tasksDir)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tasksDir, "task-*.log")) {
            for (Path file : stream) {
                String key = file.getFileName().toString();
                if (!seenTaskFiles.add(key)) {
                    continue;
                }
                if (Files.getLastModifiedTime(file).toInstant().isBefore(startedAt.minusSeconds(2))) {
                    continue;
                }
                String content = Files.readString(file, StandardCharsets.UTF_8);
                emit("terminal_chunk", Map.of(
                        "toolCallId", key,
                        "toolName", key,
                        "stream", "stdout",
                        "text", content
                ));
                ToolRun run = new ToolRun();
                run.setSessionId(portalSessionId);
                run.setToolCallId(key);
                run.setToolName(key);
                run.setStatus("completed");
                run.setOutput(content);
                run.setFinishedAt(Instant.now());
                toolRunRepository.save(run);
                emit("tool_call", Map.of(
                        "toolCallId", key,
                        "toolName", key,
                        "status", "completed",
                        "args", "{}",
                        "toolRunId", run.getId().toString()
                ));
            }
        } catch (IOException e) {
            log.debug("pollTasks: {}", e.getMessage());
        }
    }

    private void pollTranscript(Path transcript) {
        if (!Files.isRegularFile(transcript)) {
            return;
        }
        try {
            long size = Files.size(transcript);
            if (size <= transcriptByteOffset) {
                return;
            }
            try (var channel = Files.newByteChannel(transcript, StandardOpenOption.READ)) {
                channel.position(transcriptByteOffset);
                byte[] buf = new byte[(int) Math.min(size - transcriptByteOffset, 1024 * 512)];
                int read = channel.read(java.nio.ByteBuffer.wrap(buf));
                if (read <= 0) {
                    return;
                }
                transcriptByteOffset += read;
                String chunk = new String(buf, 0, read, StandardCharsets.UTF_8);
                for (String line : chunk.split("\\R")) {
                    if (line.isBlank()) {
                        continue;
                    }
                    handleTranscriptLine(line);
                }
            }
        } catch (Exception e) {
            log.debug("pollTranscript: {}", e.getMessage());
        }
    }

    private void handleTranscriptLine(String line) {
        try {
            JsonNode node = mapper.readTree(line);
            String type = firstText(node, "type", "kind", "role", "event");
            String text = extractTranscriptText(node);
            if (text == null || text.isBlank()) {
                return;
            }
            String lower = Objects.toString(type, "").toLowerCase(Locale.ROOT);
            if (lower.contains("tool") || lower.contains("command") || node.has("toolName") || node.has("tool_name")) {
                String toolName = firstText(node, "toolName", "tool_name", "name", "title");
                if (toolName == null) {
                    toolName = "tool";
                }
                String toolCallId = firstText(node, "toolCallId", "tool_call_id", "id");
                if (toolCallId == null) {
                    toolCallId = UUID.randomUUID().toString();
                }
                emit("tool_call", Map.of(
                        "toolCallId", toolCallId,
                        "toolName", toolName,
                        "status", "running",
                        "args", node.has("args") ? node.get("args").toString() : "{}"
                ));
                emit("terminal_chunk", Map.of(
                        "toolCallId", toolCallId,
                        "toolName", toolName,
                        "stream", "stdout",
                        "text", text + "\n"
                ));
            } else if (lower.contains("planner") || lower.contains("assistant") || lower.contains("model")
                    || lower.contains("response") || type == null || type.isBlank()) {
                appendAssistantDelta(text);
            }
        } catch (Exception e) {
            // Non-JSON transcript noise — ignore
        }
    }

    private String extractTranscriptText(JsonNode node) {
        if (node.has("content")) {
            JsonNode c = node.get("content");
            if (c.isTextual()) {
                return c.asText();
            }
            if (c.isObject() && c.has("text")) {
                return c.path("text").asText();
            }
            if (c.isObject() && c.has("content")) {
                return c.path("content").asText(null);
            }
        }
        return firstText(node, "text", "message", "output", "PLANNER_RESPONSE");
    }

    private Optional<String> recoverLatestAssistantFromMessages(Instant startedAt) {
        if (conversationId == null || conversationId.isBlank()) {
            return Optional.empty();
        }
        Path messagesDir = brainRoot().resolve("brain").resolve(conversationId)
                .resolve(".system_generated").resolve("messages");
        if (!Files.isDirectory(messagesDir)) {
            return Optional.empty();
        }
        try (Stream<Path> files = Files.list(messagesDir)) {
            return files
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .filter(p -> {
                        try {
                            return !Files.getLastModifiedTime(p).toInstant().isBefore(startedAt.minusSeconds(5));
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                    .map(p -> {
                        try {
                            JsonNode node = mapper.readTree(p.toFile());
                            String title = node.path("renderDetails").path("messageTitle").asText("");
                            if (title.toLowerCase(Locale.ROOT).contains("finished")) {
                                return null;
                            }
                            String content = node.path("content").asText(null);
                            return content != null && !content.isBlank() ? content : null;
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private synchronized void appendAssistantDelta(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        assistantBuffer.append(text);
        emit("assistant_delta", Map.of("text", text));
        UUID msgId = streamingAssistantMessageId.get();
        if (msgId == null) {
            ChatMessage m = new ChatMessage();
            m.setSessionId(portalSessionId);
            m.setRole(MessageRole.ASSISTANT);
            m.setContent(assistantBuffer.toString());
            m.setSequenceNo(messageRepository.maxSequence(portalSessionId) + 1);
            m = messageRepository.save(m);
            streamingAssistantMessageId.set(m.getId());
        } else {
            messageRepository.findById(msgId).ifPresent(m -> {
                m.setContent(assistantBuffer.toString());
                messageRepository.save(m);
            });
        }
    }

    private synchronized void flushAssistantMessage() {
        if (assistantBuffer.isEmpty()) {
            return;
        }
        UUID msgId = streamingAssistantMessageId.get();
        if (msgId != null) {
            messageRepository.findById(msgId).ifPresent(m -> {
                m.setContent(assistantBuffer.toString());
                messageRepository.save(m);
            });
        }
        emit("assistant_message", Map.of("content", assistantBuffer.toString()));
        streamingAssistantMessageId.set(null);
        assistantBuffer.setLength(0);
    }

    @Override
    public synchronized void cancel() {
        Process p = activeProcess.get();
        if (p != null && p.isAlive()) {
            long pid = p.pid();
            p.destroy();
            try {
                if (!p.waitFor(2, TimeUnit.SECONDS)) {
                    p.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                p.destroyForcibly();
            }
            // PowerShell launcher may leave agy.exe behind — kill tree.
            if (isWindows()) {
                try {
                    new ProcessBuilder("taskkill", "/F", "/T", "/PID", Long.toString(pid))
                            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                            .redirectError(ProcessBuilder.Redirect.DISCARD)
                            .start()
                            .waitFor(3, TimeUnit.SECONDS);
                } catch (Exception ignored) {
                }
            }
        }
        stopWatcher();
        running.set(false);
        setStatus(SessionStatus.CANCELLED);
        emit("run_cancelled", Map.of());
    }

    @Override
    public void resolvePermission(UUID permissionId, String decision, String reason) {
        throw new UnsupportedOperationException(
                "Antigravity does not support mid-turn permission prompts in this portal version. "
                        + "Runs use --dangerously-skip-permissions when enabled.");
    }

    @Override
    public synchronized void close() {
        cancel();
    }

    private Path brainRoot() {
        String configured = properties.getAntigravity().getBrainRoot();
        if (configured == null || configured.isBlank()) {
            return Path.of(System.getProperty("user.home"), ".gemini", "antigravity-cli");
        }
        String expanded = configured.replace("${user.home}", System.getProperty("user.home"));
        return Path.of(expanded).toAbsolutePath().normalize();
    }

    private void setStatus(SessionStatus status) {
        sessionRepository.findById(portalSessionId).ifPresent(s -> {
            s.setStatus(status);
            sessionRepository.save(s);
            emit("status", Map.of("status", status.name()));
        });
    }

    private void emit(String type, Map<String, Object> payload) {
        try {
            AgentEventEntity entity = new AgentEventEntity();
            entity.setSessionId(portalSessionId);
            entity.setType(type);
            entity.setPayloadJson(mapper.writeValueAsString(payload));
            eventRepository.save(entity);
            eventBus.publish(portalSessionId, new AgentEventDto(portalSessionId, type, payload, Instant.now()));
        } catch (Exception e) {
            log.warn("Failed to emit {}: {}", type, e.getMessage());
        }
    }

    private static String stripAnsi(String input) {
        return input.replaceAll("\\u001B\\[[0-9;]*[A-Za-z]", "").replace("\r", "");
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String f : fields) {
            if (node.has(f) && !node.get(f).isNull()) {
                String v = node.get(f).asText(null);
                if (v != null && !v.isBlank()) {
                    return v;
                }
            }
        }
        return null;
    }
}
