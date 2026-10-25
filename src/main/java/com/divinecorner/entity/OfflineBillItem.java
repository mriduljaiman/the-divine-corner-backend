package com.divinecorner.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Line items in an offline bill
 */
@Entity
@Table(name = "offline_bill_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfflineBillItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private OfflineBill bill;

    /** Null for items typed by hand in a manual due entry (not tied to inventory). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    private String productName;  // Snapshot at time of sale

    private String productSku;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal unitPrice;  // Price actually charged (after bargaining)

    private BigDecimal mrp;        // Listed selling price at time of sale

    private BigDecimal costPrice;  // Purchasing price snapshot, for profit reports

    private BigDecimal discount;

    @Column(nullable = false)
    private BigDecimal subtotal;  // quantity * unitPrice - discount
}
