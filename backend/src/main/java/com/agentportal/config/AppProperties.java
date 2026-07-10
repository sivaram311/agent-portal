package com.agentportal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Cors cors = new Cors();
    private final Security security = new Security();
    private final Webhooks webhooks = new Webhooks();

    public Cors getCors() {
        return cors;
    }

    public Security getSecurity() {
        return security;
    }

    public Webhooks getWebhooks() {
        return webhooks;
    }

    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:4200");

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Security {
        private String apiKey = "";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public boolean isEnabled() {
            return apiKey != null && !apiKey.isBlank();
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
}
