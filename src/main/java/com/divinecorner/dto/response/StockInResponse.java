package com.divinecorner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockInResponse {

    private UUID id;

    private UUID productId;

    private String productName;

    private String productSku;

    private Integer quantity;

    private Integer balanceAfter;  // Stock balance after this entry

    private LocalDateTime movementDate;

    private UUID supplierId;

    private String supplierName;

    private String invoiceNumber;

    private BigDecimal unitCost;

    private BigDecimal totalValue;  // quantity * unitCost

    private String remarks;

    private String createdByName;  // Admin who made the entry

    private LocalDateTime createdAt;
}
