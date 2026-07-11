package com.agentportal.repo;

import com.agentportal.domain.PlatformAgentMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlatformAgentMessageRepository extends JpaRepository<PlatformAgentMessage, UUID> {
    List<PlatformAgentMessage> findByProjectSlugOrderByCreatedAtDesc(String projectSlug);

    List<PlatformAgentMessage> findByProjectSlugAndToRoleOrderByCreatedAtDesc(String projectSlug, String toRole);

    List<PlatformAgentMessage> findByToRoleAndStatusOrderByCreatedAtDesc(String toRole, String status);

    List<PlatformAgentMessage> findAllByOrderByCreatedAtDesc();
}
