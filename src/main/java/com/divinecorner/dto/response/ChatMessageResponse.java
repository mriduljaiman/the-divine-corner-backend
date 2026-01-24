package com.divinecorner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private UUID id;
    private String sessionId;
    private String message;
    private String messageType; // USER or ADMIN
    private Boolean isRead;
    private LocalDateTime createdAt;
    private UserBasicInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserBasicInfo {
        private UUID id;
        private String firstName;
        private String lastName;
    }
}
