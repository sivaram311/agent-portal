package com.agentportal.config;

import com.agentportal.domain.PlatformApp;
import com.agentportal.domain.PortLease;
import com.agentportal.repo.PlatformAppRepository;
import com.agentportal.repo.PortLeaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PlatformRegistrySeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlatformRegistrySeeder.class);

    private final PortLeaseRepository portLeaseRepository;
    private final PlatformAppRepository platformAppRepository;

    public PlatformRegistrySeeder(
            PortLeaseRepository portLeaseRepository,
            PlatformAppRepository platformAppRepository
    ) {
        this.portLeaseRepository = portLeaseRepository;
        this.platformAppRepository = platformAppRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedPort(80, "nginx-http", "Deployment", "active", "Cloudflare Flexible → origin");
        seedPort(443, "nginx-https", "Deployment", "reserved", "Enable when origin TLS ready");
        seedPort(4200, "agent-portal-ui", "agent-portal", "active", "ng serve / static UI");
        seedPort(5432, "postgres", "docker/local", "active", "Optional portal postgres profile");
        seedPort(8080, "agent-portal-api", "agent-portal", "active", "Spring Boot JAR");
        seedPort(8082, "filebridge", "agent-portal/workspaces/FileBridge", "active", "Sample app");
        seedPort(8091, "stack-pilot", "stack-pilot", "active", "Remote/ops tooling");
        seedPort(9000, "css-auth", "centralized-security-system", "active", "SSO + JWKS");

        seedApp("agent-portal", "Agent Portal", "agent-portal", "prod",
                "https://delena.buzz/", "https://delena.buzz/api/health", "delena.buzz", 4200,
                "Autonomous agent sessions (Cursor + Antigravity)");
        seedApp("css", "Centralized Security", "agent-portal", "prod",
                "https://delena.buzz/auth/login", "http://127.0.0.1:9000/actuator/health", "delena.buzz", 9000,
                "SSO / JWT authority");
        seedApp("agent-api", "Agent API bridge", "agent-portal", "sandbox",
                "https://delena.buzz/api/agent/actions", "https://delena.buzz/api/health", null, 8080,
                "HTTP/WS control plane for external AIs");

        log.info("Platform registry seed complete (ports={}, apps={})",
                portLeaseRepository.count(), platformAppRepository.count());
    }

    private void seedPort(int port, String service, String owner, String status, String notes) {
        PortLease lease = portLeaseRepository.findByPort(port).orElseGet(PortLease::new);
        boolean isNew = lease.getId() == null;
        lease.setPort(port);
        lease.setService(service);
        lease.setOwnerApp(owner);
        lease.setEnv("host");
        lease.setBindAddress("0.0.0.0");
        // Keep human claims; only normalize seeder rows / fill gaps
        if (isNew || "seeder".equals(lease.getClaimedBy()) || "host".equalsIgnoreCase(lease.getStatus())) {
            lease.setStatus(status);
            lease.setClaimedBy("seeder");
            lease.setNotes(notes);
            if (isNew) {
                lease.setClaimedAt(Instant.now());
            }
            portLeaseRepository.save(lease);
        }
    }

    private void seedApp(
            String slug,
            String name,
            String clientId,
            String env,
            String baseUrl,
            String healthUrl,
            String subdomain,
            Integer upstreamPort,
            String description
    ) {
        if (platformAppRepository.findBySlug(slug).isPresent()) {
            return;
        }
        PlatformApp app = new PlatformApp();
        app.setSlug(slug);
        app.setName(name);
        app.setClientId(clientId);
        app.setEnv(env);
        app.setBaseUrl(baseUrl);
        app.setHealthUrl(healthUrl);
        app.setSubdomain(subdomain);
        app.setUpstreamPort(upstreamPort);
        app.setEnabled(true);
        app.setDescription(description);
        platformAppRepository.save(app);
    }
}
