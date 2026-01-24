package com.divinecorner.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private Integer stockQuantity;
    private UUID categoryId;
    private List<String> images;
    private String sku;
    private String barcode;
    private String brand;
    private Boolean active;
    private Boolean featured;
    private Integer reorderPoint;
    private Integer minStockLevel;
}
