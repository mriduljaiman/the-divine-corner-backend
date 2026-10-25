package com.divinecorner.repository;

import com.divinecorner.entity.OfflineBill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OfflineBillRepository extends JpaRepository<OfflineBill, UUID> {

    boolean existsByBillNumber(String billNumber);

    @Query("SELECT b FROM OfflineBill b WHERE (:includeVoided = true OR b.voided = false) AND b.billDate >= :from AND b.billDate < :to " +
            "AND (:q IS NULL OR b.customerPhone LIKE CONCAT('%', :q, '%') " +
            "OR LOWER(b.customerName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(b.billNumber) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<OfflineBill> search(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                             @Param("q") String q, @Param("includeVoided") boolean includeVoided, Pageable pageable);

    List<OfflineBill> findByCustomerPhoneOrderByBillDateDesc(String customerPhone);

    List<OfflineBill> findByVoidedFalseAndAmountDueGreaterThan(BigDecimal amount);

    List<OfflineBill> findByCustomerPhoneAndVoidedFalseAndAmountDueGreaterThanOrderByBillDateAsc(String customerPhone, BigDecimal amount);

    /** phone, name, total due, bills with due, last bill date */
    @Query("SELECT b.customerPhone, MAX(b.customerName), SUM(b.amountDue), COUNT(b), MAX(b.billDate) " +
            "FROM OfflineBill b WHERE b.voided = false AND b.amountDue > 0 GROUP BY b.customerPhone ORDER BY SUM(b.amountDue) DESC")
    List<Object[]> findDueSummary();

    @Query("SELECT COALESCE(SUM(b.amountDue), 0) FROM OfflineBill b WHERE b.voided = false AND b.amountDue > 0")
    BigDecimal totalOutstandingDue();

    /** total, paid at counter, bills count */
    @Query("SELECT COALESCE(SUM(b.total), 0), COALESCE(SUM(b.paidAtBilling), 0), COUNT(b) " +
            "FROM OfflineBill b WHERE b.voided = false AND b.billDate >= :from AND b.billDate < :to")
    List<Object[]> totals(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT b.billDate, b.total FROM OfflineBill b WHERE b.voided = false AND b.billDate >= :from AND b.billDate < :to")
    List<Object[]> dailyRows(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Gross profit; items with unknown cost count as zero profit. */
    @Query("SELECT COALESCE(SUM(i.subtotal - COALESCE(i.costPrice * i.quantity, i.subtotal)), 0) " +
            "FROM OfflineBillItem i WHERE i.bill.voided = false AND i.bill.billDate >= :from AND i.bill.billDate < :to")
    BigDecimal grossProfit(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT i.product.id, MAX(i.productName), SUM(i.quantity), SUM(i.subtotal) " +
            "FROM OfflineBillItem i WHERE i.bill.voided = false AND i.bill.billDate >= :from AND i.bill.billDate < :to GROUP BY i.product.id")
    List<Object[]> productSales(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
