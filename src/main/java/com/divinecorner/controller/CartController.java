package com.divinecorner.controller;

import com.divinecorner.dto.*;
import com.divinecorner.dto.AddToCartRequest;
import com.divinecorner.dto.UpdateCartItemRequest;
import com.divinecorner.dto.response.ApiResponse;
import com.divinecorner.dto.response.CartResponse;
import com.divinecorner.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(cartService.addToCart(userId, request));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateCartItem(
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(cartService.updateCartItem(userId, itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> removeFromCart(
            @PathVariable UUID itemId,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(cartService.removeFromCart(userId, itemId));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse> clearCart(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Cart cleared successfully")
                .build());
    }
}