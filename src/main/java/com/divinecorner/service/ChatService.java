package com.divinecorner.service;

import com.divinecorner.dto.request.SendChatMessageRequest;
import com.divinecorner.dto.response.ChatMessageResponse;
import com.divinecorner.entity.ChatMessage;
import com.divinecorner.entity.User;
import com.divinecorner.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public ChatMessageResponse sendMessage(SendChatMessageRequest request, User user) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        ChatMessage message = ChatMessage.builder()
                .user(user)
                .sessionId(sessionId)
                .message(request.getMessage())
                .messageType(ChatMessage.MessageType.USER)
                .build();

        message = chatMessageRepository.save(message);
        return mapToResponse(message);
    }

    @Transactional
    public ChatMessageResponse sendAdminMessage(String sessionId, String messageText) {
        ChatMessage message = ChatMessage.builder()
                .sessionId(sessionId)
                .message(messageText)
                .messageType(ChatMessage.MessageType.ADMIN)
                .build();

        message = chatMessageRepository.save(message);
        return mapToResponse(message);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getSessionMessages(String sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void markAsRead(UUID messageId) {
        chatMessageRepository.findById(messageId).ifPresent(msg -> {
            msg.setIsRead(true);
            chatMessageRepository.save(msg);
        });
    }

    private ChatMessageResponse mapToResponse(ChatMessage msg) {
        return ChatMessageResponse.builder()
                .id(msg.getId())
                .sessionId(msg.getSessionId())
                .message(msg.getMessage())
                .messageType(msg.getMessageType().name())
                .isRead(msg.getIsRead())
                .createdAt(msg.getCreatedAt())
                .user(msg.getUser() != null ? ChatMessageResponse.UserBasicInfo.builder()
                        .id(msg.getUser().getId())
                        .firstName(msg.getUser().getFirstName())
                        .lastName(msg.getUser().getLastName())
                        .build() : null)
                .build();
    }
}
