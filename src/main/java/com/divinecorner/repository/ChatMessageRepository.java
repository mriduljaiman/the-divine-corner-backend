package com.divinecorner.repository;

import com.divinecorner.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    List<ChatMessage> findByUserIdOrderByCreatedAtDesc(UUID userId);

    long countBySessionIdAndIsReadFalseAndMessageType(String sessionId, ChatMessage.MessageType messageType);

    List<ChatMessage> findByIsReadFalseAndMessageTypeOrderByCreatedAtAsc(ChatMessage.MessageType messageType);
}
