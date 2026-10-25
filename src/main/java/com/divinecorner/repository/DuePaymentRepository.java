package com.divinecorner.repository;

import com.divinecorner.entity.DuePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface DuePaymentRepository extends JpaRepository<DuePayment, UUID> {

    List<DuePayment> findByBillIdOrderByPaidAtAsc(UUID billId);

    List<DuePayment> findByBillIdIn(java.util.Collection<UUID> billIds);

    /** Cash actually received (write-offs are forgiven amounts, not money in). */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM DuePayment p WHERE p.paidAt >= :from AND p.paidAt < :to " +
            "AND (p.paymentMethod IS NULL OR p.paymentMethod <> 'WRITE_OFF')")
    BigDecimal collectedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
