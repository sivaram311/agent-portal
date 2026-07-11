package com.agentportal.web;

import com.agentportal.dto.ClaimPortRequest;
import com.agentportal.dto.PlatformAppDto;
import com.agentportal.dto.PortLeaseDto;
import com.agentportal.service.PlatformRegistryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/platform")
public class PlatformController {

    private final PlatformRegistryService platformRegistryService;

    public PlatformController(PlatformRegistryService platformRegistryService) {
        this.platformRegistryService = platformRegistryService;
    }

    @GetMapping("/ports")
    public List<PortLeaseDto> listPorts(@RequestParam(defaultValue = "true") boolean activeOnly) {
        return platformRegistryService.listPorts(activeOnly);
    }

    @PostMapping("/ports/claim")
    public PortLeaseDto claim(@Valid @RequestBody ClaimPortRequest request) {
        return platformRegistryService.claim(request);
    }

    @PostMapping("/ports/{port}/release")
    public PortLeaseDto release(@PathVariable int port) {
        return platformRegistryService.release(port);
    }

    /** CSS App Home data source — enabled production/sandbox apps. */
    @GetMapping("/apps")
    public List<PlatformAppDto> listApps(@RequestParam(defaultValue = "true") boolean enabledOnly) {
        return platformRegistryService.listApps(enabledOnly);
    }

    @GetMapping("/home")
    public Map<String, Object> home() {
        return Map.of(
                "title", "App Home",
                "auth", "CSS JWT required for mutations; list is authenticated when CSS enabled",
                "apps", platformRegistryService.listApps(true),
                "docs", "docs/platform/CSS-APP-HOME.md"
        );
    }
}
