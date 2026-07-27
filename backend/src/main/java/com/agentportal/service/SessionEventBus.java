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
        boolean isWebhookEvent = "run_completed".equals(type) || "run_failed".equals(type)
                || "input_required".equals(type) || "run_cancelled".equals(type);
        // permission_required / plan_required are the events that actually carry a
        // permissionId a human can act on (Cursor ACP tool/plan approval) --
        // input_required above is Antigravity's free-text nudge and never has one.
        // Mobile push cares about both sets; the existing external webhook contract
        // (webhookService.publish) is left unchanged, only push gets the extra two.
        boolean isPushEvent = isWebhookEvent || "permission_required".equals(type) || "plan_required".equals(type);
        if (isWebhookEvent || isPushEvent) {
            Map<String, Object> payload = event.payload() == null ? Map.of() : event.payload();
            if (isWebhookEvent) {
                webhookService.publish(type, sessionId.toString(), payload);
            }
            agentSessionRepository.findById(sessionId).ifPresent(session ->
                    pushNotificationService.notifyOwner(session.getOwnerUsername(), type, sessionId.toString(), payload));
        }
    }
}
