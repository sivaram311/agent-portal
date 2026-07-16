package com.agentportal.machine;

import com.agentportal.acp.AgentProcessManager;
import com.agentportal.config.AppProperties;
import com.agentportal.domain.AgentSession;
import com.agentportal.domain.SessionStatus;
import com.agentportal.dto.CreateSessionRequest;
import com.agentportal.dto.MachineChatRequest;
import com.agentportal.dto.MachineChatResponse;
import com.agentportal.dto.MessageDto;
import com.agentportal.dto.PromptRequest;
import com.agentportal.dto.SessionDto;
import com.agentportal.dto.UpdateSessionRoleRequest;
import com.agentportal.repo.AgentSessionRepository;
import com.agentportal.security.CurrentUser;
import com.agentportal.service.SessionService;
import com.agentportal.service.WorkspacePathResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MachineChatService {

    private final AppProperties appProperties;
    private final MachineModeService modeService;
    private final MachineContextService contextService;
    private final SessionService sessionService;
    private final AgentSessionRepository sessionRepository;
    private final WorkspacePathResolver workspacePathResolver;
    private final ObjectMapper objectMapper;

    public MachineChatService(
            AppProperties appProperties,
            MachineModeService modeService,
            MachineContextService contextService,
            SessionService sessionService,
            AgentSessionRepository sessionRepository,
            WorkspacePathResolver workspacePathResolver,
            ObjectMapper objectMapper
    ) {
        this.appProperties = appProperties;
        this.modeService = modeService;
        this.contextService = contextService;
        this.sessionService = sessionService;
        this.sessionRepository = sessionRepository;
        this.workspacePathResolver = workspacePathResolver;
        this.objectMapper = objectMapper;
    }

    public MachineChatResponse chat(MachineChatRequest request, String headerMaxMode) throws Exception {
        if (!appProperties.getMachineGateway().isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Machine Gateway disabled");
        }
        MachineMode mode = modeService.resolveEffectiveMode(request.mode(), headerMaxMode);
        String platformRole = mode.platformRole();

        String workspaceRel = appProperties.getMachineGateway().getWorkspacePath();
        Path workspace = workspacePathResolver.resolve(workspaceRel);
        Files.createDirectories(workspace.resolve("runs"));

        SessionDto session = resolveSession(request, workspaceRel, platformRole);
        if (session.platformRole() == null || !platformRole.equalsIgnoreCase(session.platformRole())) {
            session = sessionService.updatePlatformRole(session.id(),
                    new UpdateSessionRoleRequest(platformRole, null));
        }

        Map<String, Object> ctx = contextService.buildContext();
        String compactCtx = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(ctx);
        if (compactCtx.length() > 12_000) {
            compactCtx = compactCtx.substring(0, 12_000) + "\n… [truncated]";
        }

        String agentPrompt = """
                [Machine Gateway mode: %s — platformRole: %s]
                Write any files only under runs/%s/ inside this workspace (or the configured sandbox).
                Never mass-kill processes by name; kill only via LocalPort → PID allowlisted shapes.
                Do not escalate privilege beyond this mode.

                ## Live machine context (redacted)
                %s

                ## User message
                %s
                """.formatted(
                mode.name().toLowerCase(Locale.ROOT),
                platformRole,
                session.id(),
                compactCtx,
                request.message()
        );

        MessageDto userMessage = sessionService.prompt(session.id(), new PromptRequest(agentPrompt));
        return new MachineChatResponse(
                session.id(),
                mode.name().toLowerCase(Locale.ROOT),
                platformRole,
                session.workspacePath(),
                userMessage,
                "accepted"
        );
    }

    private SessionDto resolveSession(MachineChatRequest request, String workspaceRel, String platformRole)
            throws Exception {
        if (request.sessionId() != null) {
            SessionDto existing = sessionService.get(request.sessionId());
            if (existing.status() == SessionStatus.ARCHIVED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session is archived");
            }
            return existing;
        }

        String user = CurrentUser.usernameOrAnonymous();
        List<AgentSession> owned = sessionRepository
                .findByOwnerUsernameAndStatusNotOrderByUpdatedAtDesc(user, SessionStatus.ARCHIVED);
        Path expectedWs = workspacePathResolver.resolve(workspaceRel).toAbsolutePath().normalize();
        for (AgentSession s : owned) {
            if (s.getWorkspacePath() == null) {
                continue;
            }
            Path sessionWs = Path.of(s.getWorkspacePath()).toAbsolutePath().normalize();
            boolean sameWs = sessionWs.equals(expectedWs);
            boolean gatewayTitle = s.getTitle() != null && s.getTitle().startsWith("Machine Gateway");
            if (sameWs && (platformRole.equalsIgnoreCase(s.getPlatformRole()) || gatewayTitle)) {
                return sessionService.get(s.getId());
            }
        }

        String provider = request.provider() == null || request.provider().isBlank()
                ? appProperties.getMachineGateway().getDefaultProvider()
                : request.provider();
        // Machine Gateway v0: Cursor ACP only — Antigravity print-mode bypasses MachineToolGuard
        if (AgentProcessManager.normalizeProvider(provider).equals("antigravity")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Machine Gateway chat requires provider=cursor (Antigravity print-mode bypasses tool guards)");
        }
        provider = "cursor";
        return sessionService.create(new CreateSessionRequest(
                "Machine Gateway (" + platformRole + ")",
                workspaceRel,
                provider,
                true,
                platformRole,
                null
        ));
    }
}
