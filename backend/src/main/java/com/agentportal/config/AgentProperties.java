package com.agentportal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    private final Cursor cursor = new Cursor();
    private final Antigravity antigravity = new Antigravity();
    private final Workspace workspace = new Workspace();
    private boolean defaultAutoApprove = false;

    public Cursor getCursor() {
        return cursor;
    }

    public Antigravity getAntigravity() {
        return antigravity;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public boolean isDefaultAutoApprove() {
        return defaultAutoApprove;
    }

    public void setDefaultAutoApprove(boolean defaultAutoApprove) {
        this.defaultAutoApprove = defaultAutoApprove;
    }

    public static class Cursor {
        private String command = "agent";
        private String apiKey = "";
        private String model = "";
        /**
         * Hard budget for ACP process start (initialize + authenticate + session/new|load).
         * Kept short so external MCP clients fail fast instead of hanging on accept. Must stay
         * below the caller's own accept timeout (e.g. the MCP bridge's MCP_PORTAL_ACCEPT_TIMEOUT_MS,
         * default 10s) with headroom for session resolution + context build on top.
         */
        private long startTimeoutSeconds = 7;
        /** Max seconds for session/load before falling back to session/new. */
        private long sessionLoadTimeoutSeconds = 4;
        /**
         * Extra seconds beyond startTimeoutSeconds before the outer watchdog in
         * AgentProcessManager gives up and returns an error even if the start path is stuck in a
         * non-cancellable blocking call (e.g. ProcessBuilder.start() itself hanging). This bounds
         * the *entire* getOrStart() call, not just the individual ACP JSON-RPC round trips.
         */
        private long startWatchdogBufferSeconds = 2;

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public long getStartTimeoutSeconds() {
            return startTimeoutSeconds;
        }

        public void setStartTimeoutSeconds(long startTimeoutSeconds) {
            this.startTimeoutSeconds = startTimeoutSeconds;
        }

        public long getSessionLoadTimeoutSeconds() {
            return sessionLoadTimeoutSeconds;
        }

        public void setSessionLoadTimeoutSeconds(long sessionLoadTimeoutSeconds) {
            this.sessionLoadTimeoutSeconds = sessionLoadTimeoutSeconds;
        }

        public long getStartWatchdogBufferSeconds() {
            return startWatchdogBufferSeconds;
        }

        public void setStartWatchdogBufferSeconds(long startWatchdogBufferSeconds) {
            this.startWatchdogBufferSeconds = startWatchdogBufferSeconds;
        }
    }

    public static class Antigravity {
        private String command = "agy";
        private String brainRoot = "";
        private boolean skipPermissions = true;
        /** Soft interactive: detect question-like replies and emit input_required. */
        private boolean interactiveMode = true;
        /**
         * soft = question detection + follow-up -p;
         * auto = probe CLI for --acp / interactive flags and prefer them when present.
         */
        private String interactiveProtocol = "auto";
        private String printTimeout = "5m";
        private long pollIntervalMs = 500;
        private String model = "";
        /** When true and CLI supports ACP, prefer long-lived ACP over print-mode. */
        private boolean preferAcp = true;

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public String getBrainRoot() {
            return brainRoot;
        }

        public void setBrainRoot(String brainRoot) {
            this.brainRoot = brainRoot;
        }

        public boolean isSkipPermissions() {
            return skipPermissions;
        }

        public void setSkipPermissions(boolean skipPermissions) {
            this.skipPermissions = skipPermissions;
        }

        public boolean isInteractiveMode() {
            return interactiveMode;
        }

        public void setInteractiveMode(boolean interactiveMode) {
            this.interactiveMode = interactiveMode;
        }

        public String getInteractiveProtocol() {
            return interactiveProtocol;
        }

        public void setInteractiveProtocol(String interactiveProtocol) {
            this.interactiveProtocol = interactiveProtocol;
        }

        public String getPrintTimeout() {
            return printTimeout;
        }

        public void setPrintTimeout(String printTimeout) {
            this.printTimeout = printTimeout;
        }

        public long getPollIntervalMs() {
            return pollIntervalMs;
        }

        public void setPollIntervalMs(long pollIntervalMs) {
            this.pollIntervalMs = pollIntervalMs;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public boolean isPreferAcp() {
            return preferAcp;
        }

        public void setPreferAcp(boolean preferAcp) {
            this.preferAcp = preferAcp;
        }
    }

    public static class Workspace {
        private String root = "./workspaces";
        /**
         * Extra absolute roots where {@code workspacePath} may live outside {@link #root}.
         * Empty = sandbox-only. Bound from {@code agent.workspace.allowed-roots} /
         * {@code AGENT_WORKSPACE_ALLOWED_ROOTS} (comma-separated).
         */
        private List<String> allowedRoots = new ArrayList<>();
        /** 0 = unlimited. */
        private long quotaBytesPerUser = 0;

        public String getRoot() {
            return root;
        }

        public void setRoot(String root) {
            this.root = root;
        }

        public List<String> getAllowedRoots() {
            return allowedRoots;
        }

        public void setAllowedRoots(List<String> allowedRoots) {
            this.allowedRoots = new ArrayList<>();
            if (allowedRoots == null) {
                return;
            }
            for (String entry : allowedRoots) {
                if (entry == null) {
                    continue;
                }
                for (String part : entry.split(",")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        this.allowedRoots.add(trimmed);
                    }
                }
            }
        }

        public long getQuotaBytesPerUser() {
            return quotaBytesPerUser;
        }

        public void setQuotaBytesPerUser(long quotaBytesPerUser) {
            this.quotaBytesPerUser = quotaBytesPerUser;
        }
    }
}
