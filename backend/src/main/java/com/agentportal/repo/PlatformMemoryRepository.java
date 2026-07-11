package com.agentportal.repo;

import com.agentportal.domain.PlatformMemoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlatformMemoryRepository extends JpaRepository<PlatformMemoryEntry, UUID> {
    List<PlatformMemoryEntry> findByProjectSlugOrderByUpdatedAtDesc(String projectSlug);

    List<PlatformMemoryEntry> findByProjectSlugAndKindOrderByUpdatedAtDesc(String projectSlug, String kind);

    Optional<PlatformMemoryEntry> findByProjectSlugAndEntryKey(String projectSlug, String entryKey);

    List<PlatformMemoryEntry> findAllByOrderByUpdatedAtDesc();
}
