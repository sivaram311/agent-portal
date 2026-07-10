package com.agentportal.web;

import com.agentportal.config.AgentProperties;
import com.agentportal.config.AppProperties;
import com.agentportal.config.CssProperties;
import com.agentportal.service.AntigravityCapabilityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final AgentProperties agentProperties;
    private final AppProperties appProperties;
    private final CssProperties cssProperties;
    private final AntigravityCapabilityService antigravityCapabilityService;

    public HealthController(
            AgentProperties agentProperties,
            AppProperties appProperties,
            CssProperties cssProperties,
            AntigravityCapabilityService antigravityCapabilityService
    ) {
        this.agentProperties = agentProperties;
        this.appProperties = appProperties;
        this.cssProperties = cssProperties;
        this.antigravityCapabilityService = antigravityCapabilityService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        String cursorCmd = agentProperties.getCursor().getCommand();
        boolean cursorExists = Files.exists(Path.of(cursorCmd)) || "agent".equalsIgnoreCase(cursorCmd);

        String agyCmd = agentProperties.getAntigravity().getCommand();
        boolean agyExists = Files.exists(Path.of(agyCmd)) || "agy".equalsIgnoreCase(agyCmd);

        Path brain = resolveBrainRoot();
        boolean brainReadable = Files.isDirectory(brain);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "ok");
        body.put("workspaceRoot", agentProperties.getWorkspace().getRoot());

        body.put("cursorCommand", cursorCmd);
        body.put("cursorCommandExists", cursorExists);
        body.put("apiKeyConfigured", agentProperties.getCursor().getApiKey() != null
                && !agentProperties.getCursor().getApiKey().isBlank());

        body.put("antigravityCommand", agyCmd);
        body.put("antigravityCommandExists", agyExists);
        body.put("antigravityBrainRoot", brain.toString());
        body.put("antigravityBrainReadable", brainReadable);
        body.put("antigravitySkipPermissions", agentProperties.getAntigravity().isSkipPermissions());
        body.put("antigravityInteractiveMode", agentProperties.getAntigravity().isInteractiveMode());
        body.put("antigravityInteractiveProtocol", agentProperties.getAntigravity().getInteractiveProtocol());
        body.put("antigravityCapabilities", antigravityCapabilityService.probe());
        body.put("portalApiKeyRequired", appProperties.getSecurity().isEnabled());
        body.put("cssEnabled", cssProperties.isEnabled());
        body.put("cssClientId", cssProperties.getClientId());
        body.put("capabilities", Map.of(
                "cursorPermissions", true,
                "cursorSubagentAbandonChildOnly", true,
                "antigravitySoftInteractive", agentProperties.getAntigravity().isInteractiveMode(),
                "antigravityMidTurnPermissions", false,
                "sessionOwnership", cssProperties.isEnabled(),
                "websocketAuth", cssProperties.isEnabled(),
                "auditApi", true,
                "codePreview", true,
                "htmlPreview", true
        ));
        return body;
    }

    private Path resolveBrainRoot() {
        String configured = agentProperties.getAntigravity().getBrainRoot();
        if (configured == null || configured.isBlank()) {
            return Path.of(System.getProperty("user.home"), ".gemini", "antigravity-cli");
        }
        String expanded = configured.replace("${user.home}", System.getProperty("user.home"));
        return Path.of(expanded).toAbsolutePath().normalize();
    }
}
