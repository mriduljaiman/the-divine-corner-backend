package com.divinecorner.service;

import com.divinecorner.dto.*;
import com.divinecorner.dto.AddToCartRequest;
import com.divinecorner.dto.UpdateCartItemRequest;
import com.divinecorner.dto.response.CartItemResponse;
import com.divinecorner.dto.response.CartResponse;
import com.divinecorner.dto.response.ProductResponse;
import com.divinecorner.entity.*;
import com.divinecorner.exception.BadRequestException;
import com.divinecorner.exception.NotFoundException;
import com.divinecorner.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductService productService;

    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId) {
        Cart cart = getOrCreateCart(userId);
        return mapToResponse(cart);
    }

    @Transactional
    public CartResponse addToCart(UUID userId, AddToCartRequest request) {
        log.info("User Id - {}", userId);
        Cart cart = getOrCreateCart(userId);
        Product product = productService.findProductById(request.getProductId());

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new BadRequestException("Insufficient stock");
        }

        CartItem existingItem = cartItemRepository.findByCartAndProduct(cart, product).orElse(null);

        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + request.getQuantity();
            if (product.getStockQuantity() < newQuantity) {
                throw new BadRequestException("Insufficient stock");
            }
            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem cartItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cart.getItems().add(cartItem);
            cartItemRepository.save(cartItem);
        }

        return mapToResponse(cart);
    }

    @Transactional
    public CartResponse updateCartItem(UUID userId, UUID itemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Cart item does not belong to user");
        }

        if (item.getProduct().getStockQuantity() < request.getQuantity()) {
            throw new BadRequestException("Insufficient stock");
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        return mapToResponse(cart);
    }

    @Transactional
    public CartResponse removeFromCart(UUID userId, UUID itemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Cart item does not belong to user");
        }

        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        return mapToResponse(cart);
    }

    @Transactional
    public void clearCart(UUID userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteByCart(cart);
        cart.getItems().clear();
    }

    private Cart getOrCreateCart(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return cartRepository.findByUser(user)
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));
    }

    private CartResponse mapToResponse(Cart cart) {
        BigDecimal total = cart.getItems().stream()
                .map(item -> {
                    BigDecimal price = item.getProduct().getDiscountPrice() != null ?
                            item.getProduct().getDiscountPrice() : item.getProduct().getPrice();
                    return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .items(cart.getItems().stream().map(this::mapToItemResponse).toList())
                .totalAmount(total)
                .build();
    }

    private CartItemResponse mapToItemResponse(CartItem item) {
        BigDecimal price = item.getProduct().getDiscountPrice() != null ?
                item.getProduct().getDiscountPrice() : item.getProduct().getPrice();

        return CartItemResponse.builder()
                .id(item.getId())
                .product(mapToProductResponse(item.getProduct()))
                .quantity(item.getQuantity())
                .subtotal(price.multiply(BigDecimal.valueOf(item.getQuantity())))
                .build();
    }

    private ProductResponse mapToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .discountPrice(product.getDiscountPrice())
                .images(product.getImages())
                .stockQuantity(product.getStockQuantity())
                .build();
    }
}