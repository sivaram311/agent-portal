package com.agentportal.repo;

import com.agentportal.domain.PortLease;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortLeaseRepository extends JpaRepository<PortLease, UUID> {
    Optional<PortLease> findByPort(int port);

    List<PortLease> findByStatusOrderByPortAsc(String status);

    List<PortLease> findAllByOrderByPortAsc();
}
