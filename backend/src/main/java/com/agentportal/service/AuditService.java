package com.agentportal.service;

import com.agentportal.domain.AuditEvent;
import com.agentportal.repo.AuditEventRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(String action, String sessionId, String details) {
        AuditEvent event = new AuditEvent();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        event.setUsername(auth != null && auth.isAuthenticated() ? String.valueOf(auth.getPrincipal()) : "anonymous");
        event.setAction(action);
        event.setSessionId(sessionId);
        event.setDetails(details);
        repository.save(event);
    }
}
