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

    @PostMapping("/{id}/subagents/{subId}/abandon")
    public Map<String, Object> abandonSubagent(@PathVariable UUID id, @PathVariable String subId) {
        return sessionService.abandonSubagent(id, subId);
    }

    @GetMapping("/{id}/files")
    public List<FileEntryDto> listFiles(
            @PathVariable UUID id,
            @RequestParam(value = "path", required = false, defaultValue = "") String path
    ) throws Exception {
        return sessionService.listFiles(id, path);
    }

    @GetMapping("/{id}/files/content")
    public FileContentDto readFile(
            @PathVariable UUID id,
            @RequestParam("path") String path
    ) throws Exception {
        return sessionService.readFile(id, path);
    }

    @GetMapping("/{id}/changes")
    public List<FileChangeDto> changes(@PathVariable UUID id) throws Exception {
        return sessionService.listChanges(id);
    }

    @GetMapping("/{id}/changes/diff")
    public FileChangeDto changeDiff(
            @PathVariable UUID id,
            @RequestParam("path") String path
    ) throws Exception {
        return sessionService.diffFile(id, path);
    }

    @PostMapping("/{id}/changes/accept")
    public Map<String, Object> acceptChange(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body
    ) throws Exception {
        return sessionService.acceptChange(id, body.get("path"));
    }

    @PostMapping("/{id}/changes/reject")
    public Map<String, Object> rejectChange(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body
    ) throws Exception {
        return sessionService.rejectChange(id, body.get("path"));
    }

    @GetMapping("/{id}/events")
    public List<Map<String, Object>> events(@PathVariable UUID id) {
        return sessionService.events(id);
    }

    @GetMapping("/{id}/collaborators")
    public List<Map<String, Object>> collaborators(@PathVariable UUID id) {
        return sessionService.listCollaborators(id);
    }

    @PostMapping("/{id}/collaborators")
    public Map<String, Object> addCollaborator(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body
    ) {
        return sessionService.addCollaborator(id, body.get("username"));
    }

    @DeleteMapping("/{id}/collaborators/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeCollaborator(@PathVariable UUID id, @PathVariable String username) {
        sessionService.removeCollaborator(id, username);
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
