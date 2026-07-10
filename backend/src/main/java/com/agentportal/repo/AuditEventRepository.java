package com.agentportal.repo;

import com.agentportal.domain.AuditEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    List<AuditEvent> findBySessionIdOrderByCreatedAtDesc(String sessionId, Pageable pageable);

    List<AuditEvent> findByUsernameOrderByCreatedAtDesc(String username, Pageable pageable);
}
