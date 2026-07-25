package com.agentportal.service;

import com.agentportal.dto.AgentEventDto;
import com.agentportal.repo.AgentSessionRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class SessionEventBus {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebhookService webhookService;
    private final AgentSessionRepository agentSessionRepository;
    private final PushNotificationService pushNotificationService;

    public SessionEventBus(
            SimpMessagingTemplate messagingTemplate,
            WebhookService webhookService,
            AgentSessionRepository agentSessionRepository,
            PushNotificationService pushNotificationService
    ) {
        this.messagingTemplate = messagingTemplate;
        this.webhookService = webhookService;
        this.agentSessionRepository = agentSessionRepository;
        this.pushNotificationService = pushNotificationService;
    }

    public void publish(UUID sessionId, AgentEventDto event) {
        messagingTemplate.convertAndSend("/topic/sessions/" + sessionId, event);
        String type = event.type();
        if ("run_completed".equals(type) || "run_failed".equals(type) || "input_required".equals(type)
                || "run_cancelled".equals(type)) {
            Map<String, Object> payload = event.payload() == null ? Map.of() : event.payload();
            webhookService.publish(type, sessionId.toString(), payload);
            agentSessionRepository.findById(sessionId).ifPresent(session ->
                    pushNotificationService.notifyOwner(session.getOwnerUsername(), type, sessionId.toString(), payload));
        }
    }
}
