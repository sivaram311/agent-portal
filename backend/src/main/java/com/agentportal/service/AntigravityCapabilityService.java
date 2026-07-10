package com.agentportal.service;

import com.agentportal.config.AgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Probes the installed {@code agy} CLI for interactive / ACP flags.
 */
@Service
public class AntigravityCapabilityService {

    private static final Logger log = LoggerFactory.getLogger(AntigravityCapabilityService.class);

    private final AgentProperties agentProperties;
    private volatile Map<String, Object> cached;
    private volatile Instant cachedAt = Instant.EPOCH;

    public AntigravityCapabilityService(AgentProperties agentProperties) {
        this.agentProperties = agentProperties;
    }

    public Map<String, Object> probe() {
        if (cached != null && Instant.now().isBefore(cachedAt.plusSeconds(300))) {
            return cached;
        }
        synchronized (this) {
            if (cached != null && Instant.now().isBefore(cachedAt.plusSeconds(300))) {
                return cached;
            }
            Map<String, Object> result = new LinkedHashMap<>();
            String cmd = agentProperties.getAntigravity().getCommand();
            result.put("command", cmd);
            String help = runHelp(cmd);
            boolean supportsAcp = containsFlag(help, "--acp");
            boolean supportsPromptInteractive = containsFlag(help, "--prompt-interactive")
                    || containsFlag(help, "--interactive");
            boolean supportsStdin = help.toLowerCase(Locale.ROOT).contains("stdin")
                    || containsFlag(help, "--input");
            result.put("supportsAcp", supportsAcp);
            result.put("supportsPromptInteractive", supportsPromptInteractive);
            result.put("supportsStdinProtocol", supportsStdin);
            result.put("helpSnippet", help.length() > 400 ? help.substring(0, 400) : help);
            String protocol = agentProperties.getAntigravity().getInteractiveProtocol();
            String effective = "soft";
            if ("auto".equalsIgnoreCase(protocol) && (supportsAcp || supportsPromptInteractive)) {
                effective = supportsAcp ? "acp-available" : "prompt-interactive-available";
            } else if ("none".equalsIgnoreCase(protocol)) {
                effective = "none";
            }
            result.put("configuredProtocol", protocol);
            result.put("effectiveMode", effective);
            result.put("note", supportsAcp || supportsPromptInteractive
                    ? "CLI advertises interactive flags; portal still uses soft -p follow-ups until ACP bridge lands"
                    : "CLI appears print-mode only; portal uses soft question detection");
            cached = result;
            cachedAt = Instant.now();
            return result;
        }
    }

    private boolean containsFlag(String help, String flag) {
        return help != null && help.toLowerCase(Locale.ROOT).contains(flag.toLowerCase(Locale.ROOT));
    }

    private String runHelp(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command, "--help");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line).append('\n');
                    if (sb.length() > 20_000) {
                        break;
                    }
                }
            }
            p.waitFor(8, TimeUnit.SECONDS);
            if (p.isAlive()) {
                p.destroyForcibly();
            }
            return sb.toString();
        } catch (Exception e) {
            log.debug("agy --help probe failed: {}", e.getMessage());
            return "";
        }
    }
}
