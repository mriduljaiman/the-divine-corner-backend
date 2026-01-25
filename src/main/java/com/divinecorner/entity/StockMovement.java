package com.divinecorner.entity;

import com.divinecorner.enums.MovementType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * THE INVENTORY LEDGER - Heart of the inventory system
 * Every stock change is recorded here.
 * Current stock = SUM of all movements for a product
 */
@Entity
@Table(name = "stock_movements", indexes = {
    @Index(name = "idx_product_date", columnList = "product_id, movement_date"),
    @Index(name = "idx_movement_type", columnList = "movement_type"),
    @Index(name = "idx_reference", columnList = "reference_type, reference_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementType movementType;

    /**
     * Positive for STOCK_IN (credit)
     * Negative for STOCK_OUT (debit)
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Running balance after this movement
     * Calculated and stored for fast queries
     */
    @Column(nullable = false)
    private Integer balanceAfter;

    /**
     * Date when movement occurred
     * Can be backdated for corrections
     */
    @Column(nullable = false)
    private LocalDateTime movementDate;

    /**
     * Reference to source transaction
     * - Order ID for online sales
     * - Bill ID for offline sales
     * - Supplier invoice for stock in
     */
    private String referenceType;  // "ORDER", "OFFLINE_BILL", "SUPPLIER_INVOICE", "MANUAL"

    private String referenceId;    // UUID or invoice number

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    /**
     * Who made this entry
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    /**
     * Cost price per unit (for stock valuation)
     */
    private BigDecimal unitCost;

    /**
     * Total value of this movement
     */
    private BigDecimal totalValue;

    @Column(length = 1000)
    private String remarks;

    /**
     * Invoice/Bill number from supplier
     */
    private String invoiceNumber;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
