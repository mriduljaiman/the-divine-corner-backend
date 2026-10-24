package com.divinecorner.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {
    @NotBlank
    private String name;
    private String description;
    @NotNull @DecimalMin("0.01")
    private BigDecimal price;
    private BigDecimal discountPrice;
    private BigDecimal purchasingPrice;
    @NotNull @Min(0)
    private Integer stockQuantity;
    @NotNull
    private UUID categoryId;
    private List<String> images;
    private String sku;
    private String barcode;
    private String brand;
    private String color;
    private String variantGroupId;
    private List<String> sizes;
    private Boolean featured;
    private Integer reorderPoint;
    private Integer minStockLevel;
}
