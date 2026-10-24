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

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM DuePayment p WHERE p.paidAt >= :from AND p.paidAt < :to")
    BigDecimal collectedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
