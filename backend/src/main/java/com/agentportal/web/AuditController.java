package com.agentportal.web;

import com.agentportal.domain.AuditEvent;
import com.agentportal.repo.AuditEventRepository;
import com.agentportal.security.CurrentUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditEventRepository repository;

    public AuditController(AuditEventRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Map<String, Object>> list(
            @RequestParam(required = false) String sessionId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        int size = Math.min(Math.max(limit, 1), 200);
        List<AuditEvent> events;
        if (sessionId != null && !sessionId.isBlank()) {
            events = repository.findBySessionIdOrderByCreatedAtDesc(sessionId, PageRequest.of(0, size));
        } else if (CurrentUser.isAdmin()) {
            events = repository.findAll(PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                    .getContent();
        } else {
            String user = CurrentUser.usernameOrAnonymous();
            events = repository.findByUsernameOrderByCreatedAtDesc(user, PageRequest.of(0, size));
        }
        return events.stream().map(e -> Map.<String, Object>of(
                "id", e.getId().toString(),
                "username", e.getUsername() == null ? "" : e.getUsername(),
                "action", e.getAction(),
                "sessionId", e.getSessionId() == null ? "" : e.getSessionId(),
                "details", e.getDetails() == null ? "" : e.getDetails(),
                "createdAt", e.getCreatedAt().toString()
        )).toList();
    }
}
