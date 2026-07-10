package com.agentportal.dto;

import com.agentportal.domain.ChatMessage;
import com.agentportal.domain.MessageRole;

import java.time.Instant;
import java.util.UUID;

public record MessageDto(
        UUID id,
        UUID sessionId,
        MessageRole role,
        String content,
        long sequenceNo,
        Instant createdAt
) {
    public static MessageDto from(ChatMessage m) {
        return new MessageDto(
                m.getId(),
                m.getSessionId(),
                m.getRole(),
                m.getContent(),
                m.getSequenceNo(),
                m.getCreatedAt()
        );
    }
}
