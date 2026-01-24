package com.divinecorner.service;

import com.divinecorner.dto.*;
import com.divinecorner.dto.CreateProductRequest;
import com.divinecorner.dto.UpdateProductRequest;
import com.divinecorner.dto.response.CategoryResponse;
import com.divinecorner.dto.response.PageResponse;
import com.divinecorner.dto.response.ProductResponse;
import com.divinecorner.entity.Category;
import com.divinecorner.entity.Product;
import com.divinecorner.exception.NotFoundException;
import com.divinecorner.repository.ProductRepository;
import com.divinecorner.utils.ImageUrlUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final ImageUrlUtil imageUrlUtil;

    @Transactional
    @CacheEvict(value = {"products", "featuredProducts"}, allEntries = true)
    public ProductResponse createProduct(CreateProductRequest request) {
        Category category = categoryService.findCategoryById(request.getCategoryId());

        // ✅ IMPORTANT: Filter out empty strings and ensure list is not null
        List<String> imagesList = request.getImages() != null ?
                request.getImages().stream()
                        .filter(img -> img != null && !img.trim().isEmpty())
                        .collect(Collectors.toList())
                : new ArrayList<>();

        log.info("Creating product '{}' with {} images", request.getName(), imagesList.size());

        // Log each image URL
        for (int i = 0; i < imagesList.size(); i++) {
            log.info("Image {}: {}", i, imagesList.get(i));
        }

        // Auto-generate SKU if not provided
        String sku = request.getSku();
        if (sku == null || sku.trim().isEmpty()) {
            sku = generateSku(category);
        }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .discountPrice(request.getDiscountPrice())
                .stockQuantity(request.getStockQuantity())
                .category(category)
                .images(imagesList)  // ✅ Pass filtered list
                .sku(sku)
                .barcode(request.getBarcode())
                .brand(request.getBrand())
                .reorderPoint(request.getReorderPoint() != null ? request.getReorderPoint() : 10)
                .minStockLevel(request.getMinStockLevel() != null ? request.getMinStockLevel() : 5)
                .featured(request.getFeatured() != null && request.getFeatured())
                .active(true)
                .build();

        // ✅ Double check images are set before save
        if (product.getImages() == null) {
            product.setImages(new ArrayList<>());
        }

        log.info("Product object has {} images before save", product.getImages().size());

        product = productRepository.save(product);

        // ✅ Flush to ensure database sync
        productRepository.flush();

        log.info("Product saved with ID: {} and {} images in database",
                product.getId(),
                product.getImages() != null ? product.getImages().size() : 0);

        return mapToResponse(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAllProducts(Pageable pageable) {
        Page<Product> page = productRepository.findByActiveTrue(pageable);
        return mapToPageResponse(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProductsByFilters(
            UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice,
            String search, Pageable pageable) {
        Page<Product> page = productRepository.findByFilters(categoryId, minPrice, maxPrice, search, pageable);
        return mapToPageResponse(page);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "featuredProducts", key = "'all'")
    public List<ProductResponse> getFeaturedProducts() {
        log.info("Fetching featured products from DB");
        return productRepository.findByFeaturedTrueAndActiveTrue().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        Product product = findProductById(id);
        return mapToResponse(product);
    }

    @Transactional
    @CacheEvict(value = {"products", "featuredProducts"}, allEntries = true)
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        Product product = findProductById(id);

        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getDiscountPrice() != null) product.setDiscountPrice(request.getDiscountPrice());
        if (request.getStockQuantity() != null) product.setStockQuantity(request.getStockQuantity());
        if (request.getCategoryId() != null) {
            Category category = categoryService.findCategoryById(request.getCategoryId());
            product.setCategory(category);
        }
        if (request.getImages() != null) {
            // ✅ Clear and add new images
            List<String> filteredImages = request.getImages().stream()
                    .filter(img -> img != null && !img.trim().isEmpty())
                    .toList();

            product.getImages().clear();
            product.getImages().addAll(filteredImages);

            log.info("Updated product images: {} images", filteredImages.size());
        }
        if (request.getSku() != null) product.setSku(request.getSku());
        if (request.getBarcode() != null) product.setBarcode(request.getBarcode());
        if (request.getBrand() != null) product.setBrand(request.getBrand());
        if (request.getActive() != null) product.setActive(request.getActive());
        if (request.getFeatured() != null) product.setFeatured(request.getFeatured());
        if (request.getReorderPoint() != null) product.setReorderPoint(request.getReorderPoint());
        if (request.getMinStockLevel() != null) product.setMinStockLevel(request.getMinStockLevel());

        product = productRepository.save(product);
        return mapToResponse(product);
    }

    @Transactional
    @CacheEvict(value = {"products", "featuredProducts"}, allEntries = true)
    public void deleteProduct(UUID id) {
        Product product = findProductById(id);
        productRepository.delete(product);
    }

    public Product findProductById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    private ProductResponse mapToResponse(Product product) {

        List<String> images = imageUrlUtil.toAbsoluteUrls(product.getImages());

        boolean lowStock = product.getStockQuantity() <= product.getReorderPoint();

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .discountPrice(product.getDiscountPrice())
                .stockQuantity(product.getStockQuantity())
                .category(product.getCategory() != null ?
                        CategoryResponse.builder()
                                .id(product.getCategory().getId())
                                .name(product.getCategory().getName())
                                .skuPrefix(product.getCategory().getSkuPrefix())
                                .build() : null)
                .images(images) // ✅ absolute URLs now
                .sku(product.getSku())
                .barcode(product.getBarcode())
                .brand(product.getBrand())
                .rating(product.getRating())
                .reviewCount(product.getReviewCount())
                .active(product.getActive())
                .featured(product.getFeatured())
                .reorderPoint(product.getReorderPoint())
                .minStockLevel(product.getMinStockLevel())
                .lowStock(lowStock)
                .build();
    }


    private PageResponse<ProductResponse> mapToPageResponse(Page<Product> page) {
        return PageResponse.<ProductResponse>builder()
                .content(page.getContent().stream().map(this::mapToResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    /**
     * Auto-generates a unique SKU for a product based on category prefix
     * Format: {CATEGORY_PREFIX}-{TIMESTAMP}-{RANDOM_3_DIGITS}
     */
    private String generateSku(Category category) {
        String prefix = "PRD"; // Default prefix

        if (category != null && category.getSkuPrefix() != null && !category.getSkuPrefix().trim().isEmpty()) {
            prefix = category.getSkuPrefix().toUpperCase();
        }

        long timestamp = System.currentTimeMillis() / 1000; // Unix timestamp in seconds
        int random = (int) (Math.random() * 1000); // 0-999

        return String.format("%s-%d-%03d", prefix, timestamp, random);
    }

    /**
     * Get low stock products (stock <= reorder point)
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProducts() {
        return productRepository.findAll().stream()
                .filter(p -> p.getStockQuantity() <= p.getReorderPoint())
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Find product by SKU
     */
    @Transactional(readOnly = true)
    public ProductResponse findBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new NotFoundException("Product not found with SKU: " + sku));
        return mapToResponse(product);
    }

    /**
     * Find product by barcode
     */
    @Transactional(readOnly = true)
    public ProductResponse findByBarcode(String barcode) {
        Product product = productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new NotFoundException("Product not found with barcode: " + barcode));
        return mapToResponse(product);
    }
}