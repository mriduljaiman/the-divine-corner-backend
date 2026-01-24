package com.divinecorner.repository;

import com.divinecorner.entity.Cart;
import com.divinecorner.entity.CartItem;
import com.divinecorner.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
    void deleteByCart(Cart cart);
}