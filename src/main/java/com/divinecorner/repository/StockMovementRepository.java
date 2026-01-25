package com.divinecorner.repository;

import com.divinecorner.entity.StockMovement;
import com.divinecorner.enums.MovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    List<StockMovement> findByProductIdOrderByMovementDateDesc(UUID productId);

    Page<StockMovement> findByMovementDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("SELECT COALESCE(SUM(sm.quantity), 0) FROM StockMovement sm WHERE sm.product.id = :productId")
    Integer getCurrentStock(@Param("productId") UUID productId);

    Page<StockMovement> findByMovementType(MovementType type, Pageable pageable);

    @Query("SELECT sm.balanceAfter FROM StockMovement sm WHERE sm.product.id = :productId ORDER BY sm.movementDate DESC LIMIT 1")
    Integer getLastBalance(@Param("productId") UUID productId);

    @Query("SELECT COALESCE(SUM(sm.quantity), 0) FROM StockMovement sm WHERE sm.movementType = 'STOCK_IN' AND sm.movementDate BETWEEN :start AND :end")
    Integer getTotalStockIn(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(ABS(sm.quantity)), 0) FROM StockMovement sm WHERE sm.movementType IN ('STOCK_OUT', 'ONLINE_ORDER', 'OFFLINE_SALE') AND sm.movementDate BETWEEN :start AND :end")
    Integer getTotalStockOut(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT DISTINCT sm.product.id FROM StockMovement sm WHERE sm.movementDate < :date")
    List<UUID> getDeadStockProducts(@Param("date") LocalDateTime date);

    @Query("SELECT sm.product.id, COUNT(sm) as moveCount FROM StockMovement sm WHERE sm.movementDate >= :date GROUP BY sm.product.id ORDER BY moveCount DESC")
    List<Object[]> getFastMovingProducts(@Param("date") LocalDateTime date, Pageable pageable);

    Page<StockMovement> findByProductIdAndMovementTypeAndMovementDateBetween(
        UUID productId,
        MovementType movementType,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Pageable pageable
    );

    Page<StockMovement> findByMovementTypeAndMovementDateBetween(
        MovementType movementType,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Pageable pageable
    );
}
