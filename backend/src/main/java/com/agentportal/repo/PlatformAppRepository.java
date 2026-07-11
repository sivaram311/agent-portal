package com.agentportal.repo;

import com.agentportal.domain.PlatformApp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlatformAppRepository extends JpaRepository<PlatformApp, UUID> {
    Optional<PlatformApp> findBySlug(String slug);

    List<PlatformApp> findByEnabledTrueOrderByNameAsc();

    List<PlatformApp> findAllByOrderByNameAsc();
}
