package com.agentportal.web;

import com.agentportal.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

/**
 * Thin ProdDeck OS event intake — accept envelope, audit, acknowledge.
 * Public on DEV so ProdDeck can soft-forward without JWT.
 */
@RestController
@RequestMapping("/api/os-events")
public class OsEventsController {

    private static final int MAX_DETAILS_CHARS = 4000;

    private static final Set<String> KNOWN_TYPES = Set.of(
            "dispatch.hire.requested",
            "promote.decision",
            "crew.fabric.spawned",
            "crew.fabric.lane.done"
    );

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public OsEventsController(AuditService auditService, ObjectMapper objectMapper) {
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public Map<String, Object> ingest(@RequestBody Map<String, Object> envelope) {
        Object typeObj = envelope == null ? null : envelope.get("type");
        if (!(typeObj instanceof String type) || type.isBlank() || !KNOWN_TYPES.contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "type must be one of: " + String.join(", ", KNOWN_TYPES));
        }
        auditService.record("os.event." + type, null, truncateDetails(envelope));
        return Map.of("ok", true);
    }

    private String truncateDetails(Map<String, Object> envelope) {
        try {
            String json = objectMapper.writeValueAsString(envelope);
            if (json.length() <= MAX_DETAILS_CHARS) {
                return json;
            }
            return json.substring(0, MAX_DETAILS_CHARS) + "…";
        } catch (Exception e) {
            return "{\"type\":\"" + envelope.get("type") + "\",\"error\":\"serialize_failed\"}";
        }
    }
}
