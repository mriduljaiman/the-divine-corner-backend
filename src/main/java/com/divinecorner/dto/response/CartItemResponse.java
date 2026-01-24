package com.divinecorner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import java.util.UUID;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder @NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private UUID id;
    private ProductResponse product;
    private Integer quantity;
    private BigDecimal subtotal;
}
