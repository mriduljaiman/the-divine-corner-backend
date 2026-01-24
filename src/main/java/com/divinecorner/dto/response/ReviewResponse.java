package com.divinecorner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private UUID id;
    private UUID productId;
    private String productName;
    private UserBasicInfo user;
    private Integer rating;
    private String comment;
    private List<String> images;
    private Boolean verified;
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
