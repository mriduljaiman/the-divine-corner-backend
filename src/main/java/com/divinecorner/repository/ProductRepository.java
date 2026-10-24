package com.divinecorner.repository;

import com.divinecorner.entity.Category;
import com.divinecorner.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Page<Product> findByActiveTrue(Pageable pageable);
    Page<Product> findByCategory(Category category, Pageable pageable);
    Page<Product> findByCategoryAndActiveTrue(Category category, Pageable pageable);
    List<Product> findByFeaturedTrueAndActiveTrue();

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
            "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
            "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(p.barcode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> findByFilters(
            @Param("categoryId") UUID categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stockQuantity <= p.reorderPoint")
    List<Product> findLowStockProducts();

    long countByActiveTrue();

    List<Product> findByVariantGroupIdAndActiveTrueAndIdNot(String variantGroupId, UUID excludeId);

    Optional<Product> findBySku(String sku);

    Optional<Product> findBySkuIgnoreCase(String sku);

    List<Product> findByBarcodeIsNull();

    List<Product> findByIdIn(java.util.Collection<UUID> ids);

    boolean existsBySku(String sku);

    boolean existsByBarcode(String barcode);

    boolean existsByCategoryId(UUID categoryId);

    Optional<Product> findByBarcode(String barcode);

    void flush();
}