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
public class QuestionResponse {
    private UUID id;
    private UUID productId;
    private String productName;
    private UserBasicInfo askedBy;
    private String question;
    private String answer;
    private UserBasicInfo answeredBy;
    private LocalDateTime answeredAt;
    private Integer helpfulCount;
    private LocalDateTime createdAt;

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
