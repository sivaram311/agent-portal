package com.agentportal.repo;

import com.agentportal.domain.SessionCollaborator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionCollaboratorRepository extends JpaRepository<SessionCollaborator, UUID> {
    List<SessionCollaborator> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    List<SessionCollaborator> findByUsername(String username);

    Optional<SessionCollaborator> findBySessionIdAndUsername(UUID sessionId, String username);

    void deleteBySessionIdAndUsername(UUID sessionId, String username);
}
