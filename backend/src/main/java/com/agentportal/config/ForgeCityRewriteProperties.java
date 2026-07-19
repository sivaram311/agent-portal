package com.agentportal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "forgecity.rewrite")
public class ForgeCityRewriteProperties {

    private boolean enabled;
    private String apiKey = "";
    private long timeoutMs = 30_000;
    private int maxChars = 500;
    private int maxConcurrent = 1;
    private String workspacePath = "./workspaces/forgecity-rewrite";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public int getMaxChars() {
        return maxChars;
    }

    public void setMaxChars(int maxChars) {
        this.maxChars = maxChars;
    }

    public int getMaxConcurrent() {
        return maxConcurrent;
    }

    public void setMaxConcurrent(int maxConcurrent) {
        this.maxConcurrent = maxConcurrent;
    }

    public String getWorkspacePath() {
        return workspacePath;
    }

    public void setWorkspacePath(String workspacePath) {
        this.workspacePath = workspacePath;
    }

    public boolean isAvailable() {
        return enabled
                && apiKey != null
                && !apiKey.isBlank()
                && timeoutMs >= 1_000
                && timeoutMs <= 120_000
                && maxChars >= 1
                && maxChars <= 2_000
                && maxConcurrent >= 1
                && maxConcurrent <= 8
                && workspacePath != null
                && !workspacePath.isBlank();
    }
}
