package com.agentportal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Cors cors = new Cors();
    private final Security security = new Security();
    private final Webhooks webhooks = new Webhooks();
    private final MachineGateway machineGateway = new MachineGateway();

    public Cors getCors() {
        return cors;
    }

    public Security getSecurity() {
        return security;
    }

    public Webhooks getWebhooks() {
        return webhooks;
    }

    public MachineGateway getMachineGateway() {
        return machineGateway;
    }

    public static class Cors {
        private List<String> allowedOrigins = List.of("*");

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Security {
        private String apiKey = "";
        /**
         * When true, /api/** and /ws/** are permitAll (no JWT/API-key required).
         * Intended for PREPROD sandbox escape hatch only — never enable on prod.
         */
        private boolean openAccess = false;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public boolean isEnabled() {
            return apiKey != null && !apiKey.isBlank();
        }

        public boolean isOpenAccess() {
            return openAccess;
        }

        public void setOpenAccess(boolean openAccess) {
            this.openAccess = openAccess;
        }
    }

    public static class Webhooks {
        /** Optional POST URL for run lifecycle events. */
        private String url = "";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    /** Host Consciousness API (Machine Gateway) settings. */
    public static class MachineGateway {
        private boolean enabled = true;
        private String workspacePath = "machine-gateway";
        /** Absolute sandbox root for write allowlist (empty = skip sandbox allow, workspace only). */
        private String sandboxRoot = "";
        private int ttlSeconds = 15;
        /** Ceiling mode for API-key / default callers: observe|advise|act|ops */
        private String maxMode = "act";
        private String defaultProvider = "cursor";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getWorkspacePath() {
            return workspacePath;
        }

        public void setWorkspacePath(String workspacePath) {
            this.workspacePath = workspacePath;
        }

        public String getSandboxRoot() {
            return sandboxRoot;
        }

        public void setSandboxRoot(String sandboxRoot) {
            this.sandboxRoot = sandboxRoot;
        }

        public int getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(int ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }

        public String getMaxMode() {
            return maxMode;
        }

        public void setMaxMode(String maxMode) {
            this.maxMode = maxMode;
        }

        public String getDefaultProvider() {
            return defaultProvider;
        }

        public void setDefaultProvider(String defaultProvider) {
            this.defaultProvider = defaultProvider;
        }
    }
}
