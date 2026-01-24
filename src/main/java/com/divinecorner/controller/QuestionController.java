package com.divinecorner.controller;

import com.divinecorner.dto.request.AnswerQuestionRequest;
import com.divinecorner.dto.request.CreateQuestionRequest;
import com.divinecorner.dto.response.PageResponse;
import com.divinecorner.dto.response.QuestionResponse;
import com.divinecorner.entity.User;
import com.divinecorner.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
public class QuestionController {
    private final QuestionService questionService;

    @PostMapping
    public ResponseEntity<QuestionResponse> askQuestion(
            @Valid @RequestBody CreateQuestionRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(questionService.askQuestion(request, user));
    }

    @PutMapping("/{id}/answer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QuestionResponse> answerQuestion(
            @PathVariable UUID id,
            @Valid @RequestBody AnswerQuestionRequest request,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(questionService.answerQuestion(id, request, admin));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<PageResponse<QuestionResponse>> getProductQuestions(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(questionService.getProductQuestions(productId, PageRequest.of(page, size)));
    }

    @GetMapping("/unanswered")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<QuestionResponse>> getUnansweredQuestions() {
        return ResponseEntity.ok(questionService.getUnansweredQuestions());
    }
}
