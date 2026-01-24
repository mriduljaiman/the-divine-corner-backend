package com.divinecorner.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendChatMessageRequest {
    @NotBlank
    private String message;

    private String sessionId;
}
