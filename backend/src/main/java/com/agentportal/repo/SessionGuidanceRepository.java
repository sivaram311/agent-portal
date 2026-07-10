package com.agentportal.repo;

import com.agentportal.domain.SessionGuidance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SessionGuidanceRepository extends JpaRepository<SessionGuidance, UUID> {
    List<SessionGuidance> findBySessionIdOrderBySortOrderAscCreatedAtAsc(UUID sessionId);

    void deleteBySessionId(UUID sessionId);

    long countBySessionId(UUID sessionId);
}
