package com.divinecorner.repository;

import com.divinecorner.entity.Order;
import com.divinecorner.enums.OrderStatus;
import com.divinecorner.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findByUser(User user, Pageable pageable);
    Page<Order> findByUserId(UUID userId, Pageable pageable);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    List<Order> findTop10ByOrderByCreatedAtDesc();
    long countByStatus(OrderStatus status);
    boolean existsByOrderNumber(String orderNumber);

    /** booked online sales (everything except cancelled): total, count */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0), COUNT(o) FROM Order o " +
            "WHERE o.status <> com.divinecorner.enums.OrderStatus.CANCELLED AND o.createdAt >= :from AND o.createdAt < :to")
    List<Object[]> bookedTotals(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT o.createdAt, o.totalAmount FROM Order o " +
            "WHERE o.status <> com.divinecorner.enums.OrderStatus.CANCELLED AND o.createdAt >= :from AND o.createdAt < :to")
    List<Object[]> bookedDailyRows(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM((i.price - COALESCE(i.product.purchasingPrice, i.price)) * i.quantity), 0) FROM OrderItem i " +
            "WHERE i.order.status <> com.divinecorner.enums.OrderStatus.CANCELLED AND i.order.createdAt >= :from AND i.order.createdAt < :to")
    BigDecimal bookedGrossProfit(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT i.product.id, MAX(i.productName), SUM(i.quantity), SUM(i.price * i.quantity) FROM OrderItem i " +
            "WHERE i.order.status <> com.divinecorner.enums.OrderStatus.CANCELLED AND i.order.createdAt >= :from AND i.order.createdAt < :to " +
            "GROUP BY i.product.id")
    List<Object[]> productSales(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'DELIVERED'")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'DELIVERED' AND " +
            "o.createdAt >= :startDate AND o.createdAt <= :endDate")
    BigDecimal calculateRevenueByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}