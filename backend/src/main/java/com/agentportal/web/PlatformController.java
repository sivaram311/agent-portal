package com.agentportal.web;

import com.agentportal.dto.ClaimPortRequest;
import com.agentportal.dto.CreatePlatformAgentMessageRequest;
import com.agentportal.dto.CreatePlatformTaskRequest;
import com.agentportal.dto.E2eLoopProgressDto;
import com.agentportal.dto.LinkTaskSessionRequest;
import com.agentportal.dto.PlatformAgentMessageDto;
import com.agentportal.dto.PlatformAppDto;
import com.agentportal.dto.PlatformMemoryDto;
import com.agentportal.dto.PlatformOrgDto;
import com.agentportal.dto.PlatformPipelineDto;
import com.agentportal.dto.PlatformRoleDto;
import com.agentportal.dto.PlatformTaskDto;
import com.agentportal.dto.PortLeaseDto;
import com.agentportal.dto.RunPlatformPipelineRequest;
import com.agentportal.dto.SessionDto;
import com.agentportal.dto.SwarmTickRequest;
import com.agentportal.dto.SwarmTickResultDto;
import com.agentportal.dto.UpdatePlatformAgentMessageRequest;
import com.agentportal.dto.UpdatePlatformTaskRequest;
import com.agentportal.dto.UpsertPlatformMemoryRequest;
import com.agentportal.service.PlatformRegistryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/tasks/{id}/session")
    public PlatformTaskDto linkTaskSession(
            @PathVariable UUID id,
            @Valid @RequestBody LinkTaskSessionRequest request
    ) {
        return platformRegistryService.linkTaskSession(id, request);
    }

    @PostMapping("/tasks/{id}/invoke")
    public SessionDto invokeTaskSession(@PathVariable UUID id) {
        return platformRegistryService.invokeTaskSession(id);
    }

    @GetMapping("/roles")
    public List<PlatformRoleDto> listRoles() {
        return platformRegistryService.listRoles();
    }

    @GetMapping("/roles/{id}")
    public PlatformRoleDto getRole(@PathVariable String id) {
        return platformRegistryService.getRole(id);
    }

    @GetMapping("/org")
    public PlatformOrgDto orgDashboard() {
        return platformRegistryService.orgDashboard();
    }

    @PostMapping("/swarm/tick")
    public SwarmTickResultDto swarmTick(@RequestBody(required = false) SwarmTickRequest request) {
        return platformRegistryService.swarmTick(request == null ? new SwarmTickRequest(null) : request);
    }

    @GetMapping("/memory")
    public List<PlatformMemoryDto> listMemory(
            @RequestParam(required = false) String projectSlug,
            @RequestParam(required = false) String kind
    ) {
        return platformRegistryService.listMemory(projectSlug, kind);
    }

    @PostMapping("/memory")
    public PlatformMemoryDto upsertMemory(@Valid @RequestBody UpsertPlatformMemoryRequest request) {
        return platformRegistryService.upsertMemory(request);
    }

    @GetMapping("/memory/{id}")
    public PlatformMemoryDto getMemory(@PathVariable UUID id) {
        return platformRegistryService.getMemory(id);
    }

    @DeleteMapping("/memory/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMemory(@PathVariable UUID id) {
        platformRegistryService.deleteMemory(id);
    }

    @GetMapping("/messages")
    public List<PlatformAgentMessageDto> listMessages(
            @RequestParam(required = false) String projectSlug,
            @RequestParam(required = false) String toRole,
            @RequestParam(required = false) String status
    ) {
        return platformRegistryService.listMessages(projectSlug, toRole, status);
    }

    @PostMapping("/messages")
    public PlatformAgentMessageDto createMessage(@Valid @RequestBody CreatePlatformAgentMessageRequest request) {
        return platformRegistryService.createMessage(request);
    }

    @PatchMapping("/messages/{id}")
    public PlatformAgentMessageDto updateMessage(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePlatformAgentMessageRequest request
    ) {
        return platformRegistryService.updateMessage(id, request);
    }

    @GetMapping("/pipelines")
    public List<PlatformPipelineDto> listPipelines() {
        return platformRegistryService.listPipelines();
    }

    @PostMapping("/pipelines/{id}/run")
    public List<PlatformTaskDto> runPipeline(
            @PathVariable String id,
            @Valid @RequestBody RunPlatformPipelineRequest request
    ) {
        return platformRegistryService.runPipeline(id, request);
    }

    @GetMapping("/pipelines/runs/{runId}")
    public E2eLoopProgressDto getE2eLoopProgress(@PathVariable UUID runId) {
        return platformRegistryService.getE2eLoopProgress(runId);
    }

    @GetMapping("/home")
    public Map<String, Object> home() {
        return Map.of(
                "title", "App Home",
                "auth", "CSS JWT required for mutations; list is authenticated when CSS enabled",
                "apps", platformRegistryService.listApps(true),
                "pipelines", platformRegistryService.listPipelines(),
                "org", platformRegistryService.orgDashboard(),
                "docs", "docs/platform/CSS-APP-HOME.md"
        );
    }
}
