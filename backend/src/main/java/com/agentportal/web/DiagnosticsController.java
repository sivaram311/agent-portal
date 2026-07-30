package com.agentportal.web;

import com.agentportal.dto.ClientDiagnosticsRequest;
import com.agentportal.service.DiagnosticsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/diagnostics")
public class DiagnosticsController {

    private final DiagnosticsService diagnosticsService;

    public DiagnosticsController(DiagnosticsService diagnosticsService) {
        this.diagnosticsService = diagnosticsService;
    }

    @PostMapping("/client-logs")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> uploadClientLog(@Valid @RequestBody ClientDiagnosticsRequest request) {
        return diagnosticsService.storeClientLog(request);
    }

    @GetMapping("/client-logs")
    public List<Map<String, Object>> listClientLogs(
            @RequestParam(name = "limit", defaultValue = "50") int limit
    ) {
        return diagnosticsService.listClientLogs(limit);
    }

    /**
     * Download a previously uploaded log. {@code file} is a relative path under the
     * diagnostics root, e.g. {@code 2026-07-30/admin_device_123_manual.log}
     * (URL-encoded; use {@code *} splat via query for nested paths).
     */
    @GetMapping("/client-logs/file")
    public ResponseEntity<byte[]> downloadClientLog(@RequestParam("path") String path) {
        byte[] body = diagnosticsService.readClientLog(path);
        String name = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(body);
    }
}
