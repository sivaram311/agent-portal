package com.agentportal.web;

import com.agentportal.config.AppProperties;
import com.agentportal.config.CssProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthConfigController {

    private final CssProperties cssProperties;
    private final AppProperties appProperties;

    public AuthConfigController(CssProperties cssProperties, AppProperties appProperties) {
        this.cssProperties = cssProperties;
        this.appProperties = appProperties;
    }

    @GetMapping("/config")
    public Map<String, Object> config() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cssEnabled", cssProperties.isEnabled());
        body.put("authUrl", cssProperties.getAuthUrl());
        body.put("issuer", cssProperties.getIssuer());
        body.put("clientId", cssProperties.getClientId());
        body.put("loginPath", "/auth/login");
        body.put("refreshPath", "/auth/refresh");
        body.put("authMode", normalizeAuthMode(cssProperties.getAuthMode()));
        body.put("oauthRedirectPath", normalizeRedirectPath(cssProperties.getOauthRedirectPath()));
        body.put("apiKeyFallbackEnabled", appProperties.getSecurity().isEnabled());
        return body;
    }

    private static String normalizeAuthMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return "hybrid";
        }
        String v = raw.trim().toLowerCase();
        if ("oauth".equals(v) || "hybrid".equals(v) || "password".equals(v)) {
            return v;
        }
        return "hybrid";
    }

    private static String normalizeRedirectPath(String raw) {
        if (raw == null || raw.isBlank()) {
            return "/oauth/callback";
        }
        String path = raw.trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        // Never use /auth/* — nginx proxies that to css-next.
        if (path.startsWith("/auth/") || "/auth".equals(path)) {
            return "/oauth/callback";
        }
        return path;
    }
}
