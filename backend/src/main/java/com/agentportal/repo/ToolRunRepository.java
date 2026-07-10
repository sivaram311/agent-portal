package com.agentportal.repo;

import com.agentportal.domain.ToolRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ToolRunRepository extends JpaRepository<ToolRun, UUID> {
    List<ToolRun> findBySessionIdOrderByStartedAtAsc(UUID sessionId);

    Optional<ToolRun> findBySessionIdAndToolCallId(UUID sessionId, String toolCallId);
}
