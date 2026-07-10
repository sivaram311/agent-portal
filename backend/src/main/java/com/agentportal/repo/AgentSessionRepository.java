package com.agentportal.repo;

import com.agentportal.domain.AgentSession;
import com.agentportal.domain.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgentSessionRepository extends JpaRepository<AgentSession, UUID> {
    List<AgentSession> findByStatusNotOrderByUpdatedAtDesc(SessionStatus status);
}
