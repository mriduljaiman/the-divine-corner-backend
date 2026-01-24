package com.divinecorner.controller;

import com.divinecorner.dto.request.CreateReviewRequest;
import com.divinecorner.dto.response.PageResponse;
import com.divinecorner.dto.response.ReviewResponse;
import com.divinecorner.entity.User;
import com.divinecorner.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reviewService.createReview(request, user));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<PageResponse<ReviewResponse>> getProductReviews(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reviewService.getProductReviews(productId, PageRequest.of(page, size)));
    }
}
