package com.agentportal.repo;

import com.agentportal.domain.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {
    List<DeviceToken> findByOwnerUsername(String ownerUsername);

    Optional<DeviceToken> findByToken(String token);

    void deleteByToken(String token);
}
