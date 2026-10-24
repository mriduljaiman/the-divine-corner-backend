package com.divinecorner.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A payment received later against the due amount of an offline bill.
 */
@Entity
@Table(name = "due_payments", indexes = @Index(name = "idx_due_payment_bill", columnList = "bill_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DuePayment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private OfflineBill bill;

    @Column(nullable = false)
    private BigDecimal amount;

    private String paymentMethod;

    @Column(length = 500)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by")
    private User receivedBy;

    @CreationTimestamp
    private LocalDateTime paidAt;
}
