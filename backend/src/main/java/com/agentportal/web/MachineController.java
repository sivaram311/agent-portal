package com.agentportal.web;

import com.agentportal.config.AppProperties;
import com.agentportal.dto.MachineChatRequest;
import com.agentportal.dto.MachineChatResponse;
import com.agentportal.dto.MachineGatewayRequest;
import com.agentportal.dto.MachineGatewayResponse;
import com.agentportal.machine.MachineChatService;
import com.agentportal.machine.MachineContextService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/machine")
public class MachineController {

    private final AppProperties appProperties;
    private final MachineContextService contextService;
    private final MachineChatService chatService;

    public MachineController(
            AppProperties appProperties,
            MachineContextService contextService,
            MachineChatService chatService
    ) {
        this.appProperties = appProperties;
        this.contextService = contextService;
        this.chatService = chatService;
    }

    /**
     * Canonical Machine Gateway entry: always returns redacted context;
     * if {@code message} is present, also starts chat (status=accepted).
     */
    @PostMapping
    public MachineGatewayResponse gateway(
            @RequestBody(required = false) MachineGatewayRequest request,
            @RequestHeader(value = "X-Machine-Max-Mode", required = false) String maxMode
    ) throws Exception {
        assertEnabled();
        Map<String, Object> context = contextService.buildContext();
        if (request == null || request.message() == null || request.message().isBlank()) {
            return new MachineGatewayResponse(context, null);
        }
        MachineChatResponse chat = chatService.chat(
                new MachineChatRequest(
                        request.message(),
                        request.mode(),
                        request.role(),
                        request.provider(),
                        request.sessionId()
                ),
                maxMode
        );
        return new MachineGatewayResponse(context, chat);
    }

    @GetMapping("/context")
    public Map<String, Object> context() {
        assertEnabled();
        return contextService.buildContext();
    }

    @PostMapping("/chat")
    public MachineChatResponse chat(
            @Valid @RequestBody MachineChatRequest request,
            @RequestHeader(value = "X-Machine-Max-Mode", required = false) String maxMode
    ) throws Exception {
        assertEnabled();
        return chatService.chat(request, maxMode);
    }

    private void assertEnabled() {
        if (!appProperties.getMachineGateway().isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Machine Gateway disabled");
        }
    }
}
