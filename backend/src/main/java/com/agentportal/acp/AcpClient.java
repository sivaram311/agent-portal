package com.agentportal.acp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * JSON-RPC 2.0 client over stdio for Cursor CLI {@code agent acp}.
 */
public class AcpClient implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(AcpClient.class);

    private final Process process;
    private final BufferedWriter stdin;
    private final ObjectMapper mapper;
    private final boolean logPayloads;
    private final AtomicLong nextId = new AtomicLong(1);
    private final Map<Long, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final ExecutorService reader = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "acp-reader");
        t.setDaemon(true);
        return t;
    });

    private volatile Consumer<JsonNode> updateHandler = n -> {};
    private volatile BiConsumer<Long, JsonNode> permissionHandler = (id, n) -> {};
    private volatile BiConsumer<Long, JsonNode> extensionHandler = (id, n) -> {};

    public AcpClient(Process process, ObjectMapper mapper) {
        this(process, mapper, true);
    }

    /**
     * @param logPayloads false for privacy-sensitive ephemeral sessions whose JSON-RPC
     *                    payloads must never be written to application logs
     */
    public AcpClient(Process process, ObjectMapper mapper, boolean logPayloads) {
        this.process = process;
        this.mapper = mapper;
        this.logPayloads = logPayloads;
        this.stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        reader.submit(this::readLoop);
    }

    public void onUpdate(Consumer<JsonNode> handler) {
        this.updateHandler = handler != null ? handler : n -> {};
    }

    public void onPermissionRequest(BiConsumer<Long, JsonNode> handler) {
        this.permissionHandler = handler != null ? handler : (id, n) -> {};
    }

    public void onExtensionRequest(BiConsumer<Long, JsonNode> handler) {
        this.extensionHandler = handler != null ? handler : (id, n) -> {};
    }

    public CompletableFuture<JsonNode> request(String method, ObjectNode params) {
        long id = nextId.getAndIncrement();
        ObjectNode msg = mapper.createObjectNode();
        msg.put("jsonrpc", "2.0");
        msg.put("id", id);
        msg.put("method", method);
        if (params != null) {
            msg.set("params", params);
        }
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pending.put(id, future);
        try {
            write(msg);
        } catch (IOException e) {
            pending.remove(id);
            future.completeExceptionally(e);
        }
        return future;
    }

    public void respond(long id, ObjectNode result) throws IOException {
        ObjectNode msg = mapper.createObjectNode();
        msg.put("jsonrpc", "2.0");
        msg.put("id", id);
        msg.set("result", result);
        write(msg);
    }

    public void notify(String method, ObjectNode params) throws IOException {
        ObjectNode msg = mapper.createObjectNode();
        msg.put("jsonrpc", "2.0");
        msg.put("method", method);
        if (params != null) {
            msg.set("params", params);
        }
        write(msg);
    }

    public CompletableFuture<JsonNode> initialize() {
        ObjectNode params = mapper.createObjectNode();
        params.put("protocolVersion", 1);
        ObjectNode caps = params.putObject("clientCapabilities");
        ObjectNode fs = caps.putObject("fs");
        fs.put("readTextFile", false);
        fs.put("writeTextFile", false);
        caps.put("terminal", false);
        ObjectNode info = params.putObject("clientInfo");
        info.put("name", "agent-portal");
        info.put("version", "0.1.0");
        return request("initialize", params);
    }

    public CompletableFuture<JsonNode> authenticate() {
        ObjectNode params = mapper.createObjectNode();
        params.put("methodId", "cursor_login");
        return request("authenticate", params);
    }

    public CompletableFuture<String> sessionNew(String cwd) {
        ObjectNode params = mapper.createObjectNode();
        params.put("cwd", cwd);
        params.putArray("mcpServers");
        return request("session/new", params).thenApply(node -> node.path("sessionId").asText());
    }

    public CompletableFuture<String> sessionLoad(String sessionId, String cwd) {
        ObjectNode params = mapper.createObjectNode();
        params.put("sessionId", sessionId);
        params.put("cwd", cwd);
        params.putArray("mcpServers");
        return request("session/load", params).thenApply(node -> {
            String loaded = node.path("sessionId").asText(null);
            return loaded != null && !loaded.isBlank() ? loaded : sessionId;
        });
    }

    public CompletableFuture<JsonNode> sessionPrompt(String sessionId, String text) {
        ObjectNode params = mapper.createObjectNode();
        params.put("sessionId", sessionId);
        ArrayNode prompt = params.putArray("prompt");
        ObjectNode block = prompt.addObject();
        block.put("type", "text");
        block.put("text", text);
        return request("session/prompt", params);
    }

    public void sessionCancel(String sessionId) throws IOException {
        ObjectNode params = mapper.createObjectNode();
        params.put("sessionId", sessionId);
        notify("session/cancel", params);
    }

    private synchronized void write(ObjectNode msg) throws IOException {
        String line = mapper.writeValueAsString(msg);
        stdin.write(line);
        stdin.write('\n');
        stdin.flush();
        if (logPayloads) {
            log.debug("ACP >> {}", line);
        }
    }

    private void readLoop() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                if (logPayloads) {
                    log.debug("ACP << {}", line);
                }
                try {
                    JsonNode msg = mapper.readTree(line);
                    handleMessage(msg);
                } catch (Exception e) {
                    if (logPayloads) {
                        log.warn("Failed to parse ACP line: {}", line, e);
                    } else {
                        log.warn("Failed to parse redacted ACP line");
                    }
                }
            }
        } catch (IOException e) {
            if (running.get()) {
                log.warn("ACP reader stopped: {}", e.getMessage());
            }
        } finally {
            pending.forEach((id, fut) -> fut.completeExceptionally(new IOException("ACP process closed")));
            pending.clear();
        }
    }

    private void handleMessage(JsonNode msg) {
        if (msg.has("id") && (msg.has("result") || msg.has("error"))) {
            long id = msg.path("id").asLong();
            CompletableFuture<JsonNode> fut = pending.remove(id);
            if (fut != null) {
                if (msg.has("error")) {
                    fut.completeExceptionally(new RuntimeException(msg.path("error").toString()));
                } else {
                    fut.complete(msg.path("result"));
                }
            }
            return;
        }

        String method = msg.path("method").asText("");
        JsonNode params = msg.path("params");
        Long id = msg.has("id") && !msg.path("id").isNull() ? msg.path("id").asLong() : null;

        switch (method) {
            case "session/update" -> updateHandler.accept(params);
            case "session/request_permission" -> {
                if (id != null) {
                    permissionHandler.accept(id, params);
                }
            }
            case "cursor/ask_question", "cursor/create_plan" -> {
                if (id != null) {
                    extensionHandler.accept(id, msg);
                }
            }
            case "cursor/update_todos", "cursor/task", "cursor/generate_image" -> {
                if (id != null) {
                    extensionHandler.accept(id, msg);
                } else {
                    updateHandler.accept(msg);
                }
            }
            default -> {
                if (id != null && method.startsWith("cursor/")) {
                    extensionHandler.accept(id, msg);
                } else if (!method.isBlank()) {
                    log.debug("Unhandled ACP method: {}", method);
                }
            }
        }
    }

    public boolean isAlive() {
        return process.isAlive() && running.get();
    }

    /** Immediately closes an ephemeral client and its process tree without the normal grace wait. */
    public void closeImmediately() {
        running.set(false);
        try {
            stdin.close();
        } catch (IOException ignored) {
        }
        process.descendants().forEach(handle -> {
            try {
                handle.destroyForcibly();
            } catch (RuntimeException ignored) {
            }
        });
        process.destroyForcibly();
        reader.shutdownNow();
    }

    @Override
    public void close() {
        running.set(false);
        try {
            stdin.close();
        } catch (IOException ignored) {
        }
        process.destroy();
        try {
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
        reader.shutdownNow();
    }
}
