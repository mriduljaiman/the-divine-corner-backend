package com.divinecorner.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Per-customer reminder log so the admin knows who was already reminded and when.
 */
@Entity
@Table(name = "due_reminders", uniqueConstraints = @UniqueConstraint(name = "uk_due_reminder_phone", columnNames = "customer_phone"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DueReminder {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_phone", nullable = false, length = 15)
    private String customerPhone;

    private LocalDateTime lastRemindedAt;

    @Builder.Default
    private Integer remindCount = 0;
}
