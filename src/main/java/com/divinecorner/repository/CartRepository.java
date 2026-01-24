package com.divinecorner.repository;

import com.divinecorner.entity.Cart;
import com.divinecorner.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {
    Optional<Cart> findByUser(User user);
    Optional<Cart> findByUserId(UUID userId);
}