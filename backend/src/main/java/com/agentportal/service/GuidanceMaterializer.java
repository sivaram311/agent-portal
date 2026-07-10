package com.agentportal.service;

import com.agentportal.domain.GuidanceKind;
import com.agentportal.service.GuidanceService.ResolvedGuidance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Writes Cursor-compatible rule/skill files into a session workspace and builds
 * a compact instruction prefix for Antigravity (and Cursor fallback).
 */
@Component
public class GuidanceMaterializer {

    private static final Logger log = LoggerFactory.getLogger(GuidanceMaterializer.class);
    private static final int MAX_PREFIX_CHARS = 12_000;
    private static final String MANAGED_MARKER = "<!-- agent-portal-managed -->";

    public void materialize(String workspacePath, List<GuidanceService.ResolvedGuidance> packs) {
        if (workspacePath == null || workspacePath.isBlank()) {
            return;
        }
        Path root = Path.of(workspacePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
            Path rulesDir = root.resolve(".cursor").resolve("rules");
            Path skillsDir = root.resolve(".cursor").resolve("skills");
            Files.createDirectories(rulesDir);
            Files.createDirectories(skillsDir);

            clearManagedRules(rulesDir);
            clearManagedSkills(skillsDir);

            List<String> indexLines = new ArrayList<>();
            indexLines.add("# Agent Portal guidance");
            indexLines.add("");
            indexLines.add("Enabled packs for this session (managed by Agent Portal):");
            indexLines.add("");

            for (GuidanceService.ResolvedGuidance pack : packs) {
                if (pack.kind() == GuidanceKind.RULE) {
                    writeRule(rulesDir, pack);
                    indexLines.add("- Rule: **" + pack.title() + "** (`.cursor/rules/" + pack.slug() + ".mdc`)");
                } else {
                    writeSkill(skillsDir, pack);
                    indexLines.add("- Skill: **" + pack.title() + "** (`.cursor/skills/" + pack.slug() + "/SKILL.md`)");
                }
            }

            if (packs.isEmpty()) {
                indexLines.add("_No guidance packs enabled._");
                Path agents = root.resolve("AGENTS.md");
                if (Files.exists(agents) && isManaged(agents)) {
                    Files.deleteIfExists(agents);
                }
            } else {
                indexLines.add("");
                indexLines.add(MANAGED_MARKER);
                Files.writeString(root.resolve("AGENTS.md"), String.join("\n", indexLines) + "\n", StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.warn("Failed to materialize guidance into {}: {}", root, e.getMessage());
            throw new IllegalStateException("Failed to materialize guidance: " + e.getMessage(), e);
        }
    }

    public String buildPromptPrefix(List<GuidanceService.ResolvedGuidance> packs) {
        if (packs == null || packs.isEmpty()) {
            return "";
        }
        List<GuidanceService.ResolvedGuidance> rules = packs.stream()
                .filter(p -> p.kind() == GuidanceKind.RULE)
                .sorted(Comparator.comparing(GuidanceService.ResolvedGuidance::title, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<GuidanceService.ResolvedGuidance> skills = packs.stream()
                .filter(p -> p.kind() == GuidanceKind.SKILL)
                .sorted(Comparator.comparing(GuidanceService.ResolvedGuidance::title, String.CASE_INSENSITIVE_ORDER))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("[Agent Portal guidance — follow these instructions]\n");
        if (!rules.isEmpty()) {
            sb.append("\n## Rules\n");
            for (GuidanceService.ResolvedGuidance r : rules) {
                sb.append("\n### ").append(r.title()).append("\n");
                sb.append(trimBody(r.bodyMarkdown())).append("\n");
            }
        }
        if (!skills.isEmpty()) {
            sb.append("\n## Available skills (use when relevant)\n");
            for (GuidanceService.ResolvedGuidance s : skills) {
                sb.append("- **").append(s.title()).append("**: ");
                String desc = s.description();
                if (desc != null && !desc.isBlank()) {
                    sb.append(desc.trim());
                } else {
                    sb.append(firstLine(s.bodyMarkdown()));
                }
                sb.append("\n");
            }
        }
        sb.append("\n[End guidance]\n\n");
        String out = sb.toString();
        if (out.length() > MAX_PREFIX_CHARS) {
            return out.substring(0, MAX_PREFIX_CHARS) + "\n…(guidance truncated)\n\n";
        }
        return out;
    }

    private void writeRule(Path rulesDir, GuidanceService.ResolvedGuidance pack) throws IOException {
        StringBuilder front = new StringBuilder("---\n");
        front.append("description: ").append(yamlEscape(pack.description() != null ? pack.description() : pack.title())).append("\n");
        if (pack.globs() != null && !pack.globs().isBlank()) {
            front.append("globs: ").append(pack.globs().trim()).append("\n");
            front.append("alwaysApply: false\n");
        } else {
            front.append("alwaysApply: ").append(pack.alwaysApply()).append("\n");
        }
        front.append("---\n\n");
        front.append("# ").append(pack.title()).append("\n\n");
        front.append(pack.bodyMarkdown() == null ? "" : pack.bodyMarkdown().trim()).append("\n\n");
        front.append(MANAGED_MARKER).append("\n");
        Files.writeString(rulesDir.resolve(pack.slug() + ".mdc"), front.toString(), StandardCharsets.UTF_8);
    }

    private void writeSkill(Path skillsDir, GuidanceService.ResolvedGuidance pack) throws IOException {
        Path dir = skillsDir.resolve(pack.slug());
        Files.createDirectories(dir);
        String desc = pack.description() != null && !pack.description().isBlank()
                ? pack.description().trim()
                : pack.title();
        String body = """
                ---
                name: %s
                description: >-
                  %s
                ---
                # %s

                %s

                %s
                """.formatted(
                pack.slug(),
                yamlEscape(desc),
                pack.title(),
                pack.bodyMarkdown() == null ? "" : pack.bodyMarkdown().trim(),
                MANAGED_MARKER
        );
        Files.writeString(dir.resolve("SKILL.md"), body, StandardCharsets.UTF_8);
    }

    private void clearManagedRules(Path rulesDir) throws IOException {
        if (!Files.isDirectory(rulesDir)) {
            return;
        }
        try (Stream<Path> stream = Files.list(rulesDir)) {
            for (Path p : stream.filter(f -> f.getFileName().toString().endsWith(".mdc")).toList()) {
                if (isManaged(p)) {
                    Files.deleteIfExists(p);
                }
            }
        }
    }

    private void clearManagedSkills(Path skillsDir) throws IOException {
        if (!Files.isDirectory(skillsDir)) {
            return;
        }
        try (Stream<Path> stream = Files.list(skillsDir)) {
            for (Path dir : stream.filter(Files::isDirectory).toList()) {
                Path skill = dir.resolve("SKILL.md");
                if (Files.exists(skill) && isManaged(skill)) {
                    Files.deleteIfExists(skill);
                    try (Stream<Path> children = Files.list(dir)) {
                        if (children.findAny().isEmpty()) {
                            Files.deleteIfExists(dir);
                        }
                    }
                }
            }
        }
    }

    private boolean isManaged(Path file) throws IOException {
        String text = Files.readString(file, StandardCharsets.UTF_8);
        return text.contains(MANAGED_MARKER);
    }

    private static String trimBody(String body) {
        if (body == null) {
            return "";
        }
        String t = body.trim();
        if (t.length() > 4000) {
            return t.substring(0, 4000) + "\n…";
        }
        return t;
    }

    private static String firstLine(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        return body.lines().map(String::trim).filter(l -> !l.isEmpty()).findFirst().orElse("");
    }

    private static String yamlEscape(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ').trim();
    }
}
