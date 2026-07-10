package com.agentportal.service;

import com.agentportal.dto.AgentEventDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SessionEventBus {

    private final SimpMessagingTemplate messagingTemplate;

    public SessionEventBus(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(UUID sessionId, AgentEventDto event) {
        messagingTemplate.convertAndSend("/topic/sessions/" + sessionId, event);
    }
}
