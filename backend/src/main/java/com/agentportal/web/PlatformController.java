package com.agentportal.web;

import com.agentportal.dto.ClaimPortRequest;
import com.agentportal.dto.CreatePlatformTaskRequest;
import com.agentportal.dto.PlatformAppDto;
import com.agentportal.dto.PlatformRoleDto;
import com.agentportal.dto.PlatformTaskDto;
import com.agentportal.dto.PortLeaseDto;
import com.agentportal.dto.UpdatePlatformTaskRequest;
import com.agentportal.service.PlatformRegistryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    @GetMapping("/tasks")
    public List<PlatformTaskDto> listTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role
    ) {
        return platformRegistryService.listTasks(status, role);
    }

    @PostMapping("/tasks")
    public PlatformTaskDto createTask(@Valid @RequestBody CreatePlatformTaskRequest request) {
        return platformRegistryService.createTask(request);
    }

    @GetMapping("/tasks/{id}")
    public PlatformTaskDto getTask(@PathVariable UUID id) {
        return platformRegistryService.getTask(id);
    }

    @PatchMapping("/tasks/{id}")
    public PlatformTaskDto updateTask(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePlatformTaskRequest request
    ) {
        return platformRegistryService.updateTask(id, request);
    }

    @GetMapping("/roles")
    public List<PlatformRoleDto> listRoles() {
        return platformRegistryService.listRoles();
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
