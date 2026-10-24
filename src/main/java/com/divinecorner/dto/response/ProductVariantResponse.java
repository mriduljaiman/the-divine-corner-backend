package com.divinecorner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse {
    private UUID id;
    private String color;
    private String thumbnail;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private Integer stockQuantity;
}
