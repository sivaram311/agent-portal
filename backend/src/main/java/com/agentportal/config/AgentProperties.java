package com.agentportal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

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
    }

    public static class Antigravity {
        private String command = "agy";
        private String brainRoot = "";
        private boolean skipPermissions = true;
        /** Soft interactive: detect question-like replies and emit input_required. */
        private boolean interactiveMode = true;
        private String printTimeout = "5m";
        private long pollIntervalMs = 500;

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
    }

    public static class Workspace {
        private String root = "./workspaces";

        public String getRoot() {
            return root;
        }

        public void setRoot(String root) {
            this.root = root;
        }
    }
}
