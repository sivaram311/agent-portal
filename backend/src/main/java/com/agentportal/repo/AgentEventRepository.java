package com.agentportal.repo;

import com.agentportal.domain.AgentEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgentEventRepository extends JpaRepository<AgentEventEntity, UUID> {
    List<AgentEventEntity> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}
