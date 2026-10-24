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
    private final ShippingService shippingService;

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

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new BadRequestException("This product is no longer available");
        }

        String size = normalizeSize(product, request.getSize());

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new BadRequestException("Insufficient stock");
        }

        CartItem existingItem = (size == null
                ? cartItemRepository.findByCartAndProductAndSizeIsNull(cart, product)
                : cartItemRepository.findByCartAndProductAndSize(cart, product, size)).orElse(null);

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
                    .size(size)
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

    /** Products with sizes require one of their sizes; products without sizes ignore it. */
    private String normalizeSize(Product product, String requested) {
        String sizes = product.getSizes();
        if (sizes == null || sizes.isBlank()) {
            return null;
        }
        String size = requested != null ? requested.trim() : "";
        if (size.isEmpty()) {
            throw new BadRequestException("Please select a size");
        }
        for (String s : sizes.split(",")) {
            if (s.trim().equalsIgnoreCase(size)) {
                return s.trim();
            }
        }
        throw new BadRequestException("Selected size is not available");
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

        BigDecimal shipping = cart.getItems().isEmpty()
                ? BigDecimal.ZERO
                : shippingService.calculateShippingCharges(total, null);

        return CartResponse.builder()
                .id(cart.getId())
                .items(cart.getItems().stream().map(this::mapToItemResponse).toList())
                .totalAmount(total)
                .shippingCharges(shipping)
                .grandTotal(total.add(shipping))
                .build();
    }

    private CartItemResponse mapToItemResponse(CartItem item) {
        BigDecimal price = item.getProduct().getDiscountPrice() != null ?
                item.getProduct().getDiscountPrice() : item.getProduct().getPrice();

        return CartItemResponse.builder()
                .id(item.getId())
                .product(mapToProductResponse(item.getProduct()))
                .quantity(item.getQuantity())
                .size(item.getSize())
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
                .sizes(product.getSizes() != null && !product.getSizes().isBlank()
                        ? java.util.Arrays.stream(product.getSizes().split(",")).map(String::trim)
                                .filter(s -> !s.isEmpty()).toList()
                        : java.util.List.of())
                .active(product.getActive())
                .build();
    }
}