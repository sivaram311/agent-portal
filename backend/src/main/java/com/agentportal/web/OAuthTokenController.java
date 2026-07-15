package com.agentportal.web;

import com.agentportal.config.CssProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Same-origin BFF for OAuth token exchange. Browser → portal → css-next issuer,
 * avoiding apex CORS gaps (https://delena.buzz not matched by https://*.delena.buzz).
 */
@RestController
@RequestMapping("/api/auth")
public class OAuthTokenController {

    private final CssProperties cssProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public OAuthTokenController(CssProperties cssProperties, ObjectMapper objectMapper) {
        this.cssProperties = cssProperties;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/oauth/token", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> exchange(@RequestBody Map<String, Object> body) {
        String issuer = trimSlash(cssProperties.getIssuer());
        if (issuer == null || issuer.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "issuer_missing"));
        }
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(issuer + "/oauth/token"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            MediaType contentType = MediaType.APPLICATION_JSON;
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode node = objectMapper.readTree(response.body());
                return ResponseEntity.ok().contentType(contentType).body(node);
            }
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "token_exchange_failed");
            err.put("status", response.statusCode());
            try {
                err.put("details", objectMapper.readTree(response.body()));
            } catch (Exception ignored) {
                err.put("message", response.body());
            }
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(err);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "token_exchange_failed", "message", ex.getMessage()));
        }
    }

    private static String trimSlash(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim();
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }
}
