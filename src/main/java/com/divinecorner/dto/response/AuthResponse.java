package com.divinecorner.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String message;
    private UserResponse user;

    // Add these fields
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
}
