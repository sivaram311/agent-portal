package com.agentportal.service;

import com.agentportal.domain.PlatformApp;
import com.agentportal.domain.PortLease;
import com.agentportal.dto.ClaimPortRequest;
import com.agentportal.dto.PlatformAppDto;
import com.agentportal.dto.PortLeaseDto;
import com.agentportal.repo.PlatformAppRepository;
import com.agentportal.repo.PortLeaseRepository;
import com.agentportal.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class PlatformRegistryService {

    private final PortLeaseRepository portLeaseRepository;
    private final PlatformAppRepository platformAppRepository;

    public PlatformRegistryService(
            PortLeaseRepository portLeaseRepository,
            PlatformAppRepository platformAppRepository
    ) {
        this.portLeaseRepository = portLeaseRepository;
        this.platformAppRepository = platformAppRepository;
    }

    @Transactional(readOnly = true)
    public List<PortLeaseDto> listPorts(boolean activeOnly) {
        List<PortLease> rows = activeOnly
                ? portLeaseRepository.findByStatusOrderByPortAsc("active")
                : portLeaseRepository.findAllByOrderByPortAsc();
        return rows.stream().map(PortLeaseDto::from).toList();
    }

    @Transactional
    public PortLeaseDto claim(ClaimPortRequest req) {
        String user = CurrentUser.username().orElse("anonymous");
        PortLease existing = portLeaseRepository.findByPort(req.port()).orElse(null);
        if (existing != null && "active".equalsIgnoreCase(existing.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Port " + req.port() + " already claimed by " + existing.getOwnerApp());
        }
        PortLease lease = existing != null ? existing : new PortLease();
        lease.setPort(req.port());
        lease.setService(req.service().trim());
        lease.setOwnerApp(req.ownerApp().trim());
        lease.setEnv(req.env() == null || req.env().isBlank() ? "host" : req.env().trim());
        lease.setBindAddress(req.bindAddress() == null || req.bindAddress().isBlank()
                ? "0.0.0.0" : req.bindAddress().trim());
        lease.setStatus("active");
        lease.setClaimedBy(user);
        lease.setClaimedAt(Instant.now());
        lease.setReleasedAt(null);
        lease.setNotes(req.notes());
        return PortLeaseDto.from(portLeaseRepository.save(lease));
    }

    @Transactional
    public PortLeaseDto release(int port) {
        PortLease lease = portLeaseRepository.findByPort(port)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Port not found"));
        lease.setStatus("released");
        lease.setReleasedAt(Instant.now());
        return PortLeaseDto.from(portLeaseRepository.save(lease));
    }

    @Transactional(readOnly = true)
    public List<PlatformAppDto> listApps(boolean enabledOnly) {
        List<PlatformApp> rows = enabledOnly
                ? platformAppRepository.findByEnabledTrueOrderByNameAsc()
                : platformAppRepository.findAllByOrderByNameAsc();
        return rows.stream().map(PlatformAppDto::from).toList();
    }
}
