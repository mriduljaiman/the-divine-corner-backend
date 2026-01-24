package com.divinecorner.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    private BigDecimal discountPrice;

    @Column(nullable = false)
    private Integer stockQuantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    // ✅ Initialize in field declaration
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "product_images",
            joinColumns = @JoinColumn(name = "product_id")
    )
    @Column(name = "image_url", length = 1000)
    @OrderColumn(name = "image_order")
    private List<String> images = new ArrayList<>();

    @Column(unique = true)
    private String sku;

    @Column(unique = true)
    private String barcode;

    private String brand;
    private Double rating = 0.0;
    private Integer reviewCount = 0;

    private Integer reorderPoint = 10;
    private Integer minStockLevel = 5;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Boolean featured = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ✅ Manual Builder Pattern (without Lombok @Builder)
    public static ProductBuilder builder() {
        return new ProductBuilder();
    }

    public static class ProductBuilder {
        private final Product product = new Product();

        public ProductBuilder name(String name) {
            product.name = name;
            return this;
        }

        public ProductBuilder description(String description) {
            product.description = description;
            return this;
        }

        public ProductBuilder price(BigDecimal price) {
            product.price = price;
            return this;
        }

        public ProductBuilder discountPrice(BigDecimal discountPrice) {
            product.discountPrice = discountPrice;
            return this;
        }

        public ProductBuilder stockQuantity(Integer stockQuantity) {
            product.stockQuantity = stockQuantity;
            return this;
        }

        public ProductBuilder category(Category category) {
            product.category = category;
            return this;
        }

        public ProductBuilder images(List<String> images) {
            if (images != null) {
                product.images = new ArrayList<>(images);
            }
            return this;
        }

        public ProductBuilder sku(String sku) {
            product.sku = sku;
            return this;
        }

        public ProductBuilder barcode(String barcode) {
            product.barcode = barcode;
            return this;
        }

        public ProductBuilder brand(String brand) {
            product.brand = brand;
            return this;
        }

        public ProductBuilder reorderPoint(Integer reorderPoint) {
            product.reorderPoint = reorderPoint;
            return this;
        }

        public ProductBuilder minStockLevel(Integer minStockLevel) {
            product.minStockLevel = minStockLevel;
            return this;
        }

        public ProductBuilder rating(Double rating) {
            product.rating = rating;
            return this;
        }

        public ProductBuilder reviewCount(Integer reviewCount) {
            product.reviewCount = reviewCount;
            return this;
        }

        public ProductBuilder active(Boolean active) {
            product.active = active;
            return this;
        }

        public ProductBuilder featured(Boolean featured) {
            product.featured = featured;
            return this;
        }

        public Product build() {
            // ✅ Ensure defaults
            if (product.rating == null) product.rating = 0.0;
            if (product.reviewCount == null) product.reviewCount = 0;
            if (product.active == null) product.active = true;
            if (product.featured == null) product.featured = false;
            if (product.images == null) product.images = new ArrayList<>();
            if (product.reorderPoint == null) product.reorderPoint = 10;
            if (product.minStockLevel == null) product.minStockLevel = 5;

            return product;
        }
    }
}