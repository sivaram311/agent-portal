package com.agentportal.repo;

import com.agentportal.domain.PermissionRequest;
import com.agentportal.domain.PermissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionRequestRepository extends JpaRepository<PermissionRequest, UUID> {
    List<PermissionRequest> findBySessionIdAndStatusOrderByCreatedAtDesc(UUID sessionId, PermissionStatus status);

    Optional<PermissionRequest> findByIdAndSessionId(UUID id, UUID sessionId);
}
