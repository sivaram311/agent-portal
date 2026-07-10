package com.agentportal.service;

import com.agentportal.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    public WebhookService(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    public void publish(String eventType, String sessionId, Map<String, Object> payload) {
        String url = appProperties.getWebhooks().getUrl();
        if (url == null || url.isBlank()) {
            return;
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("event", eventType);
            body.put("sessionId", sessionId);
            body.put("timestamp", Instant.now().toString());
            body.put("payload", payload == null ? Map.of() : payload);
            restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.debug("Webhook publish failed: {}", e.getMessage());
        }
    }
}
