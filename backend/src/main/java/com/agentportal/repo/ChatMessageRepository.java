package com.agentportal.repo;

import com.agentportal.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findBySessionIdOrderBySequenceNoAsc(UUID sessionId);

    @Query("select coalesce(max(m.sequenceNo), 0) from ChatMessage m where m.sessionId = :sessionId")
    long maxSequence(@Param("sessionId") UUID sessionId);

    Optional<ChatMessage> findFirstBySessionIdAndRoleOrderBySequenceNoDesc(UUID sessionId, com.agentportal.domain.MessageRole role);
}
