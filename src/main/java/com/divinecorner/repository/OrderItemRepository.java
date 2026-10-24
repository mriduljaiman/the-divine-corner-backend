package com.divinecorner.repository;

import com.divinecorner.entity.OrderItem;
import com.divinecorner.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    boolean existsByOrderUserIdAndProductIdAndOrderStatus(UUID userId, UUID productId, OrderStatus status);
}