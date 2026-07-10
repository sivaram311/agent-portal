package com.agentportal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Cors cors = new Cors();
    private final Security security = new Security();

    public Cors getCors() {
        return cors;
    }

    public Security getSecurity() {
        return security;
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
        /** When non-blank, require header X-API-Key (or Authorization: Bearer) for /api/** except /api/health. */
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
}
