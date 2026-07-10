package com.agentportal.web;

import com.agentportal.dto.*;
import com.agentportal.service.SessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public List<SessionDto> list() {
        return sessionService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionDto create(@Valid @RequestBody CreateSessionRequest request) throws Exception {
        return sessionService.create(request);
    }

    @GetMapping("/{id}")
    public SessionDto get(@PathVariable UUID id) {
        return sessionService.get(id);
    }

    @GetMapping("/{id}/messages")
    public List<MessageDto> messages(@PathVariable UUID id) {
        return sessionService.messages(id);
    }

    @GetMapping("/{id}/tools")
    public List<ToolRunDto> tools(@PathVariable UUID id) {
        return sessionService.toolRuns(id);
    }

    @GetMapping("/{id}/permissions")
    public List<PermissionDto> permissions(@PathVariable UUID id) {
        return sessionService.pendingPermissions(id);
    }

    @PostMapping("/{id}/prompt")
    public MessageDto prompt(@PathVariable UUID id, @Valid @RequestBody PromptRequest request) throws Exception {
        return sessionService.prompt(id, request);
    }

    @PostMapping("/{id}/cancel")
    public Map<String, String> cancel(@PathVariable UUID id) {
        sessionService.cancel(id);
        return Map.of("status", "cancelled");
    }

    @PostMapping("/{id}/permissions/{permissionId}")
    public Map<String, String> resolvePermission(
            @PathVariable UUID id,
            @PathVariable UUID permissionId,
            @Valid @RequestBody PermissionDecisionRequest request
    ) throws Exception {
        sessionService.resolvePermission(id, permissionId, request);
        return Map.of("status", "resolved");
    }

    @PostMapping("/{id}/archive")
    public SessionDto archive(@PathVariable UUID id) {
        return sessionService.archive(id);
    }
}
