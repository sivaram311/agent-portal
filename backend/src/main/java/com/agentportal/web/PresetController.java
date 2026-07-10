package com.agentportal.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/presets")
public class PresetController {

    @GetMapping
    public List<Map<String, Object>> list() {
        return List.of(
                Map.of(
                        "id", "cursor-refactor",
                        "title", "Cursor refactor",
                        "provider", "cursor",
                        "workspacePath", "demo",
                        "starterPrompt", "Review this workspace and propose a focused refactor plan."
                ),
                Map.of(
                        "id", "cursor-fix",
                        "title", "Cursor bugfix",
                        "provider", "cursor",
                        "workspacePath", "demo",
                        "starterPrompt", "Find the most likely bug in this workspace and fix it with a minimal change."
                ),
                Map.of(
                        "id", "agy-explore",
                        "title", "Antigravity explore",
                        "provider", "antigravity",
                        "workspacePath", "demo",
                        "starterPrompt", "Explore this workspace and summarize architecture, entry points, and risks."
                ),
                Map.of(
                        "id", "agy-docs",
                        "title", "Antigravity docs",
                        "provider", "antigravity",
                        "workspacePath", "demo",
                        "starterPrompt", "Draft a concise README section describing how to run and test this project."
                )
        );
    }
}
