package com.divinecorner.controller;

import com.divinecorner.dto.request.SendChatMessageRequest;
import com.divinecorner.dto.response.ChatMessageResponse;
import com.divinecorner.entity.User;
import com.divinecorner.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping("/send")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @Valid @RequestBody SendChatMessageRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.sendMessage(request, user));
    }

    @PostMapping("/admin/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChatMessageResponse> sendAdminMessage(
            @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(chatService.sendAdminMessage(
                request.get("sessionId"),
                request.get("message")
        ));
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<ChatMessageResponse>> getSessionMessages(@PathVariable String sessionId) {
        return ResponseEntity.ok(chatService.getSessionMessages(sessionId));
    }
}
