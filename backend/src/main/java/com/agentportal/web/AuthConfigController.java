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
        body.put("clientId", cssProperties.getClientId());
        body.put("loginPath", "/auth/login");
        body.put("refreshPath", "/auth/refresh");
        body.put("apiKeyFallbackEnabled", appProperties.getSecurity().isEnabled());
        return body;
    }
}
