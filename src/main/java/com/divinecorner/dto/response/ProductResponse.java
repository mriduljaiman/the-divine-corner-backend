package com.divinecorner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder @NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private Integer stockQuantity;
    private CategoryResponse category;
    private List<String> images;
    private String sku;
    private String barcode;
    private String brand;
    private Double rating;
    private Integer reviewCount;
    private Boolean active;
    private Boolean featured;
    private Integer reorderPoint;
    private Integer minStockLevel;
    private Boolean lowStock;
}