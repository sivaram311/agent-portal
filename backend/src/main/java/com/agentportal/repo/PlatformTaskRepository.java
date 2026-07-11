package com.agentportal.repo;

import com.agentportal.domain.PlatformTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlatformTaskRepository extends JpaRepository<PlatformTask, UUID> {
    List<PlatformTask> findAllByOrderByCreatedAtDesc();

    List<PlatformTask> findByStatusOrderByCreatedAtDesc(String status);

    List<PlatformTask> findByRoleOrderByCreatedAtDesc(String role);

    List<PlatformTask> findByStatusAndRoleOrderByCreatedAtDesc(String status, String role);

    List<PlatformTask> findByParentTaskIdOrderByCreatedAtAsc(UUID parentTaskId);

    List<PlatformTask> findByProjectSlugOrderByCreatedAtDesc(String projectSlug);
}
