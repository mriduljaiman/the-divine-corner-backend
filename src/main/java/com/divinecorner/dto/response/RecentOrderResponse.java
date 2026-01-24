package com.divinecorner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentOrderResponse {
    private UUID id;
    private String orderNumber;
    private String customerName;
    private BigDecimal totalAmount;
    private String status;
    private String createdAt;
}
