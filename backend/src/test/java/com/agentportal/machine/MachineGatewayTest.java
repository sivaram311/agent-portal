package com.agentportal.machine;

import com.agentportal.config.AppProperties;
import com.agentportal.config.AgentProperties;
import com.agentportal.service.WorkspacePathResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretRedactorTest {

    @Test
    void redactsSensitiveKeysRecursively() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("status", "ok");
        input.put("apiKey", "super-secret");
        input.put("nested", Map.of("password", "x", "port", 8080));
        input.put("list", List.of(Map.of("access_token", "abc")));

        @SuppressWarnings("unchecked")
        Map<String, Object> out = (Map<String, Object>) SecretRedactor.redact(input);
        assertEquals("ok", out.get("status"));
        assertEquals("[REDACTED]", out.get("apiKey"));
        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) out.get("nested");
        assertEquals("[REDACTED]", nested.get("password"));
        assertEquals(8080, nested.get("port"));
    }
}

class MachineToolGuardTest {

    @TempDir
    Path temp;

    private Path workspace;
    private Path sandbox;
    private MachineToolGuard guard;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        workspace = temp.resolve("ws");
        sandbox = temp.resolve("sandbox");
        AgentProperties agentProps = new AgentProperties();
        agentProps.getWorkspace().setRoot(workspace.toString());
        AppProperties appProps = new AppProperties();
        appProps.getMachineGateway().setSandboxRoot(sandbox.toString());
        guard = new MachineToolGuard(appProps, new WorkspacePathResolver(agentProps));
    }

    @Test
    void allowsEditInsideWorkspace() throws Exception {
        ObjectNode params = mapper.createObjectNode();
        params.put("path", workspace.resolve("runs/a.txt").toString());
        guard.assertEditPaths(workspace.toString(), params);
    }

    @Test
    void rejectsEditOutsideWorkspaceAndSandbox() {
        ObjectNode params = mapper.createObjectNode();
        params.put("path", temp.resolve("forbidden.txt").toString());
        SecurityException ex = assertThrows(SecurityException.class,
                () -> guard.assertEditPaths(workspace.toString(), params));
        assertTrue(ex.getMessage().contains("outside"));
    }

    @Test
    void allowsStopProcessById() throws Exception {
        ObjectNode params = mapper.createObjectNode();
        params.put("command", "Stop-Process -Id 4242");
        guard.assertShellSafe(params);
    }

    @Test
    void rejectsStopProcessByName() {
        ObjectNode params = mapper.createObjectNode();
        params.put("command", "Stop-Process -Name node -Force");
        SecurityException ex = assertThrows(SecurityException.class, () -> guard.assertShellSafe(params));
        assertTrue(ex.getMessage().contains("allowlisted"));
    }

    @Test
    void rejectsArbitraryShell() {
        ObjectNode params = mapper.createObjectNode();
        params.put("command", "whoami");
        assertThrows(SecurityException.class, () -> guard.assertShellSafe(params));
    }

    @Test
    void allowsNetTcpPortLookup() {
        ObjectNode params = mapper.createObjectNode();
        params.put("command", "Get-NetTCPConnection -LocalPort 8080");
        guard.assertShellSafe(params);
    }
}

class MachineModeServiceTest {

    @Test
    void allowsDeescalationAndBlocksEscalation() {
        AppProperties props = new AppProperties();
        props.getMachineGateway().setMaxMode("act");
        MachineModeService svc = new MachineModeService(props);
        assertEquals(MachineMode.OBSERVE, svc.resolveEffectiveMode("observe", null));
        assertEquals(MachineMode.ACT, svc.resolveEffectiveMode("act", null));
        assertThrows(Exception.class, () -> svc.resolveEffectiveMode("ops", null));
        assertEquals(MachineMode.OBSERVE, svc.resolveEffectiveMode("observe", "observe"));
    }
}
