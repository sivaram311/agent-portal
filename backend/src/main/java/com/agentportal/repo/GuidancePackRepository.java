package com.agentportal.repo;

import com.agentportal.domain.GuidanceKind;
import com.agentportal.domain.GuidancePack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuidancePackRepository extends JpaRepository<GuidancePack, UUID> {
    List<GuidancePack> findByOwnerUsernameOrderByKindAscTitleAsc(String ownerUsername);

    List<GuidancePack> findByOwnerUsernameAndKindOrderByTitleAsc(String ownerUsername, GuidanceKind kind);

    List<GuidancePack> findByOwnerUsernameAndEnabledByDefaultTrueOrderByKindAscTitleAsc(String ownerUsername);

    Optional<GuidancePack> findByIdAndOwnerUsername(UUID id, String ownerUsername);

    Optional<GuidancePack> findByOwnerUsernameAndSlug(String ownerUsername, String slug);
}
