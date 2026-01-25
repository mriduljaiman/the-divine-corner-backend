package com.divinecorner.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Offline sales made via React Native Admin App
 * or direct counter sales
 */
@Entity
@Table(name = "offline_bills", indexes = {
    @Index(name = "idx_bill_number", columnList = "bill_number", unique = true),
    @Index(name = "idx_bill_date", columnList = "bill_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfflineBill {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String billNumber;  // Format: OFF-202601241234

    @Column(nullable = false)
    private LocalDateTime billDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;  // Admin who generated the bill

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OfflineBillItem> items = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal subtotal;

    private BigDecimal discount;

    private BigDecimal tax;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(nullable = false)
    private String paymentMethod;  // CASH, UPI, CARD

    private String upiTransactionId;

    private String customerName;

    private String customerPhone;

    @Column(length = 1000)
    private String notes;

    /**
     * For offline mode support
     * TRUE if created offline and synced later
     */
    @Builder.Default
    private Boolean syncedFromDevice = false;

    private String deviceId;  // Which device created this bill

    @CreationTimestamp
    private LocalDateTime createdAt;
}
