package com.agentportal.service;

import com.agentportal.config.AgentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspacePathResolverTest {

    @TempDir
    Path temp;

    private Path sandbox;
    private Path allowedExtra;
    private WorkspacePathResolver resolver;

    @BeforeEach
    void setUp() {
        sandbox = temp.resolve("sandbox");
        allowedExtra = temp.resolve("extra-apps");
        AgentProperties props = new AgentProperties();
        props.getWorkspace().setRoot(sandbox.toString());
        props.getWorkspace().setAllowedRoots(List.of(allowedExtra.toString()));
        resolver = new WorkspacePathResolver(props);
    }

    @Test
    void relativePathResolvesUnderSandbox() {
        Path resolved = resolver.resolve("demo");
        assertEquals(sandbox.resolve("demo").normalize(), resolved);
    }

    @Test
    void absoluteUnderSandboxAllowed() {
        Path target = sandbox.resolve("demo").toAbsolutePath().normalize();
        assertEquals(target, resolver.resolve(target.toString()));
    }

    @Test
    void absoluteUnderAllowlistAllowed() {
        Path target = allowedExtra.resolve("my-app").toAbsolutePath().normalize();
        assertEquals(target, resolver.resolve(target.toString()));
    }

    @Test
    void absoluteOutsideSandboxAndAllowlistRejected() {
        Path outside = temp.resolve("forbidden").resolve("x").toAbsolutePath().normalize();
        SecurityException ex = assertThrows(SecurityException.class, () -> resolver.resolve(outside.toString()));
        assertTrue(ex.getMessage().contains("not allowed"));
    }

    @Test
    void parentSegmentsRejected() {
        assertThrows(SecurityException.class, () -> resolver.resolve("../escape"));
    }

    @Test
    void blankRejected() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("  "));
    }

    @Test
    void commaSeparatedAllowlistParsed() {
        AgentProperties props = new AgentProperties();
        props.getWorkspace().setRoot(sandbox.toString());
        props.getWorkspace().setAllowedRoots(List.of(
                allowedExtra + "," + temp.resolve("second").toAbsolutePath()));
        WorkspacePathResolver multi = new WorkspacePathResolver(props);
        Path second = temp.resolve("second").resolve("proj").toAbsolutePath().normalize();
        assertEquals(second, multi.resolve(second.toString()));
    }
}
