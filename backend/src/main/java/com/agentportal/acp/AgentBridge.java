package com.agentportal.acp;

import com.agentportal.config.AgentProperties;
import com.agentportal.domain.*;
import com.agentportal.dto.AgentEventDto;
import com.agentportal.repo.*;
import com.agentportal.service.SessionEventBus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * One Cursor ACP process bound to a portal session.
 */
public class AgentBridge implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(AgentBridge.class);

    private final UUID portalSessionId;
    private final String workspacePath;
    private final AgentProperties properties;
    private final ObjectMapper mapper;
    private final SessionEventBus eventBus;
    private final AgentSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final AgentEventRepository eventRepository;
    private final ToolRunRepository toolRunRepository;
    private final PermissionRequestRepository permissionRepository;
    private final boolean autoApprove;
    private final String acpCliCommand;
    private final String acpSubcommand;

    private AcpClient client;
    private String cursorSessionId;
    private final AtomicReference<UUID> streamingAssistantMessageId = new AtomicReference<>();
    private final StringBuilder assistantBuffer = new StringBuilder();
    private final Map<Long, UUID> pendingPermissionByAcpId = new ConcurrentHashMap<>();
    private final Map<String, UUID> toolRunByCallId = new ConcurrentHashMap<>();
    private final Set<String> abandonedToolCallIds = ConcurrentHashMap.newKeySet();

    public AgentBridge(
            UUID portalSessionId,
            String workspacePath,
            String existingCursorSessionId,
            AgentProperties properties,
            ObjectMapper mapper,
            SessionEventBus eventBus,
            AgentSessionRepository sessionRepository,
            ChatMessageRepository messageRepository,
            AgentEventRepository eventRepository,
            ToolRunRepository toolRunRepository,
            PermissionRequestRepository permissionRepository,
            boolean autoApprove
    ) {
        this(portalSessionId, workspacePath, existingCursorSessionId, properties, mapper, eventBus,
                sessionRepository, messageRepository, eventRepository, toolRunRepository, permissionRepository,
                autoApprove, null, null);
    }

    public AgentBridge(
            UUID portalSessionId,
            String workspacePath,
            String existingCursorSessionId,
            AgentProperties properties,
            ObjectMapper mapper,
            SessionEventBus eventBus,
            AgentSessionRepository sessionRepository,
            ChatMessageRepository messageRepository,
            AgentEventRepository eventRepository,
            ToolRunRepository toolRunRepository,
            PermissionRequestRepository permissionRepository,
            boolean autoApprove,
            String acpCliCommand,
            String acpSubcommand
    ) {
        this.portalSessionId = portalSessionId;
        this.workspacePath = workspacePath;
        this.cursorSessionId = existingCursorSessionId;
        this.properties = properties;
        this.mapper = mapper;
        this.eventBus = eventBus;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.eventRepository = eventRepository;
        this.toolRunRepository = toolRunRepository;
        this.permissionRepository = permissionRepository;
        this.autoApprove = autoApprove;
        this.acpCliCommand = acpCliCommand;
        this.acpSubcommand = acpSubcommand;
    }

    public synchronized void start() throws Exception {
        if (client != null && client.isAlive()) {
            return;
        }
        Path cwd = Path.of(workspacePath).toAbsolutePath().normalize();
        Files.createDirectories(cwd);

        List<String> command = buildCommand();
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(cwd.toFile());
        pb.redirectErrorStream(false);
        Map<String, String> env = pb.environment();
        String apiKey = properties.getCursor().getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            env.put("CURSOR_API_KEY", apiKey);
        }

        log.info("Starting ACP for session {} in {}", portalSessionId, cwd);
        Process process = pb.start();

        // Drain stderr so the process never blocks on a full pipe.
        Thread stderrDrain = new Thread(() -> {
            try (var err = process.getErrorStream()) {
                err.transferTo(OutputStream.nullOutputStream());
            } catch (IOException ignored) {
            }
        }, "acp-stderr-" + portalSessionId);
        stderrDrain.setDaemon(true);
        stderrDrain.start();

        client = new AcpClient(process, mapper);
        client.onUpdate(this::handleUpdate);
        client.onPermissionRequest(this::handlePermissionRequest);
        client.onExtensionRequest(this::handleExtensionRequest);

        client.initialize().get(60, TimeUnit.SECONDS);
        try {
            client.authenticate().get(60, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("ACP authenticate returned error (may already be logged in): {}", e.getMessage());
        }

        if (cursorSessionId != null && !cursorSessionId.isBlank()) {
            try {
                cursorSessionId = client.sessionLoad(cursorSessionId, cwd.toString()).get(60, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("session/load failed, creating new session: {}", e.getMessage());
                cursorSessionId = client.sessionNew(cwd.toString()).get(60, TimeUnit.SECONDS);
            }
        } else {
            cursorSessionId = client.sessionNew(cwd.toString()).get(60, TimeUnit.SECONDS);
        }

        AgentSession session = sessionRepository.findById(portalSessionId).orElseThrow();
        session.setCursorSessionId(cursorSessionId);
        session.setStatus(SessionStatus.IDLE);
        sessionRepository.save(session);

        emit("bridge_ready", Map.of(
                "cursorSessionId", cursorSessionId,
                "workspacePath", cwd.toString()
        ));
    }

    private List<String> buildCommand() {
        String cmd = acpCliCommand != null ? acpCliCommand : properties.getCursor().getCommand();
        List<String> command = new ArrayList<>();
        if (cmd.toLowerCase(Locale.ROOT).endsWith(".cmd") || cmd.toLowerCase(Locale.ROOT).endsWith(".bat")) {
            command.add("cmd.exe");
            command.add("/c");
        }
        command.add(cmd);
        boolean agyAcp = acpCliCommand != null;
        if (!agyAcp) {
            String apiKey = properties.getCursor().getApiKey();
            if (apiKey != null && !apiKey.isBlank()) {
                command.add("--api-key");
                command.add(apiKey);
            }
            String model = properties.getCursor().getModel();
            if (model != null && !model.isBlank()) {
                command.add("--model");
                command.add(model);
            }
        } else {
            String model = properties.getAntigravity().getModel();
            if (model != null && !model.isBlank()) {
                command.add("--model");
                command.add(model);
            }
        }
        command.add(acpSubcommand != null ? acpSubcommand : "acp");
        return command;
    }

    public synchronized CompletableFuture<Void> prompt(String text) {
        ensureStarted();
        setStatus(SessionStatus.STREAMING);
        streamingAssistantMessageId.set(null);
        assistantBuffer.setLength(0);

        return client.sessionPrompt(cursorSessionId, text)
                .thenAccept(result -> {
                    flushAssistantMessage();
                    String stop = result.path("stopReason").asText("end_turn");
                    setStatus(SessionStatus.IDLE);
                    emit("run_completed", Map.of("stopReason", stop));
                })
                .exceptionally(ex -> {
                    flushAssistantMessage();
                    setStatus(SessionStatus.FAILED);
                    emit("run_failed", Map.of("error", String.valueOf(ex.getMessage())));
                    return null;
                });
    }

    public synchronized void cancel() {
        if (client == null || !client.isAlive()) {
            return;
        }
        try {
            client.sessionCancel(cursorSessionId);
            setStatus(SessionStatus.CANCELLED);
            emit("run_cancelled", Map.of());
        } catch (IOException e) {
            log.warn("Cancel failed: {}", e.getMessage());
        }
    }

    /**
     * Marks the child abandoned and suppresses further terminal chunks for it.
     * Cursor ACP cannot cancel a single nested tool without ending the turn;
     * we keep the parent session alive (return true) so Abandon is child-scoped in the UI/DB.
     */
    public synchronized boolean abandonSubagent(String subagentId) {
        if (subagentId == null || subagentId.isBlank()) {
            return false;
        }
        abandonedToolCallIds.add(subagentId);
        toolRunRepository.findBySessionIdAndToolCallId(portalSessionId, subagentId).ifPresent(run -> {
            run.setStatus("abandoned");
            run.setFinishedAt(Instant.now());
            run.setKind(run.getKind() == null ? "subagent" : run.getKind());
            run.setSubagentId(subagentId);
            toolRunRepository.save(run);
        });
        emit("subagent_finished", Map.of(
                "subagentId", subagentId,
                "toolCallId", subagentId,
                "status", "abandoned",
                "kind", "subagent",
                "childOnly", true
        ));
        return true;
    }

    public synchronized void resolvePermission(UUID permissionId, String decision, String reason) throws Exception {
        PermissionRequest req = permissionRepository.findByIdAndSessionId(permissionId, portalSessionId)
                .orElseThrow(() -> new NoSuchElementException("Permission not found"));
        if (req.getStatus() != PermissionStatus.PENDING) {
            throw new IllegalStateException("Permission already resolved");
        }

        ObjectNode result = mapper.createObjectNode();
        if ("plan".equalsIgnoreCase(req.getKind())) {
            if ("accept".equalsIgnoreCase(decision) || "allow-once".equalsIgnoreCase(decision) || "allow-always".equalsIgnoreCase(decision)) {
                result.putObject("outcome").put("outcome", "accepted");
                req.setStatus(PermissionStatus.ALLOW_ONCE);
            } else {
                ObjectNode outcome = result.putObject("outcome");
                outcome.put("outcome", "rejected");
                if (reason != null) {
                    outcome.put("reason", reason);
                }
                req.setStatus(PermissionStatus.REJECT_ONCE);
            }
        } else {
            String outcome = normalizePermission(decision);
            ObjectNode outcomeNode = result.putObject("outcome");
            outcomeNode.put("outcome", "selected");
            outcomeNode.put("optionId", outcome);
            req.setStatus(switch (outcome) {
                case "allow-always" -> PermissionStatus.ALLOW_ALWAYS;
                case "reject-once" -> PermissionStatus.REJECT_ONCE;
                default -> PermissionStatus.ALLOW_ONCE;
            });
        }

        req.setResolvedAt(Instant.now());
        permissionRepository.save(req);
        client.respond(req.getAcpRequestId(), result);
        setStatus(SessionStatus.STREAMING);
        emit("permission_resolved", Map.of(
                "permissionId", permissionId.toString(),
                "decision", decision
        ));
    }

    private String normalizePermission(String decision) {
        return switch (decision.toLowerCase(Locale.ROOT)) {
            case "allow-always", "always" -> "allow-always";
            case "reject", "reject-once", "deny" -> "reject-once";
            default -> "allow-once";
        };
    }

    private void ensureStarted() {
        if (client == null || !client.isAlive()) {
            throw new IllegalStateException("ACP bridge is not running");
        }
    }

    private void handleUpdate(JsonNode params) {
        try {
            JsonNode update = params.has("update") ? params.get("update") : params;
            String sessionUpdate = update.path("sessionUpdate").asText("");
            if (sessionUpdate.isBlank() && params.has("method")) {
                handleCursorNotification(params);
                return;
            }

            switch (sessionUpdate) {
                case "agent_message_chunk", "agent_thought_chunk" -> {
                    String text = extractText(update);
                    if (text == null || text.isEmpty()) {
                        return;
                    }
                    boolean thinking = "agent_thought_chunk".equals(sessionUpdate);
                    if (!thinking) {
                        appendAssistant(text);
                    }
                    emit(thinking ? "thinking_delta" : "assistant_delta", Map.of("text", text));
                }
                case "tool_call", "tool_call_update" -> handleToolCall(update);
                default -> emit("session_update", Map.of("raw", update.toString(), "sessionUpdate", sessionUpdate));
            }
        } catch (Exception e) {
            log.warn("Failed handling session/update: {}", e.getMessage());
        }
    }

    private void handleCursorNotification(JsonNode msg) {
        String method = msg.path("method").asText();
        JsonNode params = msg.path("params");
        emit(method.replace('/', '_'), Map.of("raw", params.toString()));
        if ("cursor/task".equals(method)) {
            String desc = params.path("description").asText("task");
            emit("terminal_chunk", Map.of(
                    "stream", "task",
                    "text", "[task] " + desc + "\n"
            ));
        }
        if ("cursor/update_todos".equals(method)) {
            emit("todos_updated", Map.of("raw", params.toString()));
        }
    }

    private void handleToolCall(JsonNode update) {
        String toolCallId = firstNonBlank(
                update.path("toolCallId").asText(null),
                update.path("tool_call_id").asText(null),
                UUID.randomUUID().toString()
        );
        if (abandonedToolCallIds.contains(toolCallId)) {
            return;
        }
        String name = firstNonBlank(
                update.path("title").asText(null),
                update.path("name").asText(null),
                update.path("kind").asText(null),
                "tool"
        );
        String status = update.path("status").asText("running");
        String args = update.has("rawInput") ? update.get("rawInput").toString()
                : update.has("arguments") ? update.get("arguments").toString()
                : update.has("content") ? update.get("content").toString()
                : "{}";

        ToolRun run = toolRunRepository.findBySessionIdAndToolCallId(portalSessionId, toolCallId)
                .orElseGet(() -> {
                    ToolRun t = new ToolRun();
                    t.setSessionId(portalSessionId);
                    t.setToolCallId(toolCallId);
                    t.setToolName(name);
                    t.setArgsJson(args);
                    t.setStatus("running");
                    return toolRunRepository.save(t);
                });

        run.setToolName(name);
        run.setArgsJson(args);
        run.setStatus(status);

        String outputChunk = extractToolOutput(update);
        if (outputChunk != null && !outputChunk.isBlank()) {
            String existing = run.getOutput() == null ? "" : run.getOutput();
            run.setOutput(existing + outputChunk);
            emit("terminal_chunk", Map.of(
                    "toolCallId", toolCallId,
                    "toolName", name,
                    "stream", "stdout",
                    "text", outputChunk
            ));
        }

        if ("completed".equalsIgnoreCase(status) || "failed".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status)) {
            run.setFinishedAt(Instant.now());
            if (update.has("exitCode")) {
                run.setExitCode(update.path("exitCode").asInt());
            }
        }
        toolRunRepository.save(run);
        toolRunByCallId.put(toolCallId, run.getId());

        boolean subagent = looksLikeSubagent(name, update);
        if (subagent) {
            run.setKind("subagent");
            run.setSubagentId(toolCallId);
            if (update.has("parentToolCallId")) {
                run.setParentToolCallId(update.path("parentToolCallId").asText(null));
            }
            toolRunRepository.save(run);
            String eventType = "completed".equalsIgnoreCase(status) || "failed".equalsIgnoreCase(status)
                    || "error".equalsIgnoreCase(status) || "abandoned".equalsIgnoreCase(status)
                    ? "subagent_finished"
                    : ("running".equalsIgnoreCase(status) ? "subagent_started" : "subagent_progress");
            emit(eventType, Map.of(
                    "subagentId", toolCallId,
                    "toolCallId", toolCallId,
                    "toolName", name,
                    "status", status,
                    "toolRunId", run.getId().toString(),
                    "kind", "subagent"
            ));
        }

        emit("tool_call", Map.of(
                "toolCallId", toolCallId,
                "toolName", name,
                "status", status,
                "args", args,
                "toolRunId", run.getId().toString(),
                "kind", run.getKind() == null ? "tool" : run.getKind(),
                "subagentId", Objects.toString(run.getSubagentId(), "")
        ));
    }

    private boolean looksLikeSubagent(String name, JsonNode update) {
        String n = (name == null ? "" : name).toLowerCase(Locale.ROOT);
        String kind = update.path("kind").asText("").toLowerCase(Locale.ROOT);
        return n.contains("agent") || n.contains("subagent") || n.contains("task")
                || kind.contains("agent") || update.has("agentId") || update.has("subagentId");
    }

    private String extractToolOutput(JsonNode update) {
        if (update.has("content") && update.get("content").isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode c : update.get("content")) {
                String t = extractText(c);
                if (t != null) {
                    sb.append(t);
                }
                if (c.has("output")) {
                    sb.append(c.get("output").asText(""));
                }
                if (c.has("text")) {
                    sb.append(c.path("text").asText(""));
                }
            }
            return sb.toString();
        }
        if (update.has("rawOutput")) {
            return update.get("rawOutput").asText("");
        }
        return null;
    }

    private void handlePermissionRequest(long acpId, JsonNode params) {
        try {
            if (autoApprove) {
                ObjectNode result = mapper.createObjectNode();
                ObjectNode outcome = result.putObject("outcome");
                outcome.put("outcome", "selected");
                outcome.put("optionId", "allow-once");
                client.respond(acpId, result);
                emit("permission_auto_approved", Map.of("acpRequestId", acpId));
                return;
            }

            PermissionRequest req = new PermissionRequest();
            req.setSessionId(portalSessionId);
            req.setAcpRequestId(acpId);
            req.setToolCallId(params.path("toolCallId").asText(null));
            req.setDetailsJson(params.toString());
            req.setKind("permission");
            req.setStatus(PermissionStatus.PENDING);
            req = permissionRepository.save(req);
            pendingPermissionByAcpId.put(acpId, req.getId());
            setStatus(SessionStatus.WAITING_PERMISSION);
            emit("permission_required", Map.of(
                    "permissionId", req.getId().toString(),
                    "toolCallId", Objects.toString(req.getToolCallId(), ""),
                    "details", params.toString(),
                    "kind", "permission"
            ));
        } catch (Exception e) {
            log.error("Failed to handle permission request", e);
        }
    }

    private void handleExtensionRequest(long acpId, JsonNode msg) {
        String method = msg.path("method").asText();
        JsonNode params = msg.path("params");
        try {
            if ("cursor/create_plan".equals(method)) {
                PermissionRequest req = new PermissionRequest();
                req.setSessionId(portalSessionId);
                req.setAcpRequestId(acpId);
                req.setToolCallId(params.path("toolCallId").asText(null));
                req.setDetailsJson(params.toString());
                req.setKind("plan");
                req.setPlanMarkdown(params.path("plan").asText(""));
                req.setStatus(PermissionStatus.PENDING);
                req = permissionRepository.save(req);
                setStatus(SessionStatus.WAITING_PLAN);
                emit("plan_required", Map.of(
                        "permissionId", req.getId().toString(),
                        "plan", req.getPlanMarkdown(),
                        "name", params.path("name").asText(""),
                        "overview", params.path("overview").asText(""),
                        "kind", "plan"
                ));
                return;
            }

            if ("cursor/ask_question".equals(method)) {
                // Auto-skip questions in v1 to avoid blocking unattended runs; surface to UI.
                emit("ask_question", Map.of("raw", params.toString()));
                ObjectNode result = mapper.createObjectNode();
                ObjectNode outcome = result.putObject("outcome");
                outcome.put("outcome", "skipped");
                outcome.put("reason", "answered via portal defaults");
                client.respond(acpId, result);
                return;
            }

            // Default accept for unknown blocking extensions
            ObjectNode result = mapper.createObjectNode();
            result.putObject("outcome").put("outcome", "cancelled");
            client.respond(acpId, result);
        } catch (Exception e) {
            log.error("Failed extension request {}", method, e);
        }
    }

    private synchronized void appendAssistant(String text) {
        assistantBuffer.append(text);
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

            AgentEventDto dto = new AgentEventDto(portalSessionId, type, payload, Instant.now());
            eventBus.publish(portalSessionId, dto);
        } catch (Exception e) {
            log.warn("Failed to emit event {}: {}", type, e.getMessage());
        }
    }

    private String extractText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.has("content")) {
            JsonNode content = node.get("content");
            if (content.isTextual()) {
                return content.asText();
            }
            if (content.isObject() && content.has("text")) {
                return content.path("text").asText();
            }
            if (content.isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode c : content) {
                    String t = extractText(c);
                    if (t != null) {
                        sb.append(t);
                    }
                }
                return sb.toString();
            }
        }
        if (node.has("text")) {
            return node.path("text").asText();
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    public String getCursorSessionId() {
        return cursorSessionId;
    }

    @Override
    public synchronized void close() {
        if (client != null) {
            client.close();
            client = null;
        }
    }
}
