package com.divinecorner.service;

import com.divinecorner.dto.*;
import com.divinecorner.dto.CreateProductRequest;
import com.divinecorner.dto.UpdateProductRequest;
import com.divinecorner.dto.response.CategoryResponse;
import com.divinecorner.dto.response.PageResponse;
import com.divinecorner.dto.response.ProductResponse;
import com.divinecorner.dto.response.ProductVariantResponse;
import com.divinecorner.entity.Category;
import com.divinecorner.entity.Product;
import com.divinecorner.exception.BadRequestException;
import com.divinecorner.exception.NotFoundException;
import com.divinecorner.repository.ProductRepository;
import com.divinecorner.utils.ImageUrlUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
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

        validatePrices(request.getPrice(), request.getDiscountPrice(), request.getPurchasingPrice());

        // Auto-generate SKU if not provided
        String sku = blankToNull(request.getSku());
        if (sku == null) {
            sku = generateSku(category);
        } else if (productRepository.existsBySku(sku)) {
            throw new BadRequestException("SKU '" + sku + "' already exists");
        }
        String barcode = blankToNull(request.getBarcode());
        if (barcode != null && productRepository.existsByBarcode(barcode)) {
            throw new BadRequestException("Barcode '" + barcode + "' already exists");
        }
        if (barcode == null) {
            barcode = generateBarcode();
        }

        Product product = Product.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .discountPrice(request.getDiscountPrice())
                .purchasingPrice(request.getPurchasingPrice())
                .stockQuantity(request.getStockQuantity())
                .category(category)
                .images(imagesList)  // ✅ Pass filtered list
                .sku(sku)
                .barcode(barcode)
                .brand(blankToNull(request.getBrand()))
                .reorderPoint(request.getReorderPoint() != null ? request.getReorderPoint() : 10)
                .minStockLevel(request.getMinStockLevel() != null ? request.getMinStockLevel() : 5)
                .featured(request.getFeatured() != null && request.getFeatured())
                .color(blankToNull(request.getColor()))
                .variantGroupId(blankToNull(request.getVariantGroupId()))
                .sizes(joinSizes(request.getSizes()))
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
        String term = search != null && !search.isBlank() ? search.trim() : null;
        Page<Product> page = productRepository.findByFilters(categoryId, minPrice, maxPrice, term, pageable);
        return mapToPageResponse(page);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "featuredProducts", key = "'all'")
    public List<ProductResponse> getFeaturedProducts() {
        log.info("Fetching featured products from DB");
        // ArrayList (not Stream.toList()) so the Redis JSON serializer can deserialize it back
        return productRepository.findByFeaturedTrueAndActiveTrue().stream()
                .map(product -> mapToResponse(product, false))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        Product product = findProductById(id);
        if (!Boolean.TRUE.equals(product.getActive()) && !isCurrentUserAdmin()) {
            throw new NotFoundException("Product not found");
        }
        return mapToResponse(product);
    }

    @Transactional
    @CacheEvict(value = {"products", "featuredProducts"}, allEntries = true)
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        Product product = findProductById(id);

        if (request.getName() != null && !request.getName().isBlank()) product.setName(request.getName().trim());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        // The admin form always sends both fields; null means "cleared"
        product.setDiscountPrice(request.getDiscountPrice());
        product.setPurchasingPrice(request.getPurchasingPrice());
        validatePrices(product.getPrice(), product.getDiscountPrice(), product.getPurchasingPrice());
        if (request.getStockQuantity() != null) {
            if (request.getStockQuantity() < 0) throw new BadRequestException("Stock cannot be negative");
            product.setStockQuantity(request.getStockQuantity());
        }
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
        if (request.getSku() != null) {
            String sku = blankToNull(request.getSku());
            if (sku != null && !sku.equals(product.getSku()) && productRepository.existsBySku(sku)) {
                throw new BadRequestException("SKU '" + sku + "' already exists");
            }
            if (sku != null) product.setSku(sku);
        }
        if (request.getBarcode() != null) {
            String barcode = blankToNull(request.getBarcode());
            if (barcode != null && !barcode.equals(product.getBarcode()) && productRepository.existsByBarcode(barcode)) {
                throw new BadRequestException("Barcode '" + barcode + "' already exists");
            }
            // A blank value never removes the barcode: printed labels depend on it
            if (barcode != null) product.setBarcode(barcode);
        }
        if (request.getBrand() != null) product.setBrand(blankToNull(request.getBrand()));
        if (request.getActive() != null) product.setActive(request.getActive());
        if (request.getFeatured() != null) product.setFeatured(request.getFeatured());
        if (request.getReorderPoint() != null) product.setReorderPoint(request.getReorderPoint());
        if (request.getMinStockLevel() != null) product.setMinStockLevel(request.getMinStockLevel());
        if (request.getColor() != null) product.setColor(blankToNull(request.getColor()));
        if (request.getVariantGroupId() != null) product.setVariantGroupId(blankToNull(request.getVariantGroupId()));
        if (request.getSizes() != null) product.setSizes(joinSizes(request.getSizes()));

        product = productRepository.save(product);
        return mapToResponse(product);
    }

    @Transactional
    @CacheEvict(value = {"products", "featuredProducts"}, allEntries = true)
    public void deleteProduct(UUID id) {
        // Soft delete: the product is referenced by orders, carts and the stock ledger,
        // so a hard delete would fail (FK) or wipe order history.
        Product product = findProductById(id);
        product.setActive(false);
        product.setFeatured(false);
        productRepository.save(product);
    }

    public Product findProductById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    private ProductResponse mapToResponse(Product product) {
        return mapToResponse(product, isCurrentUserAdmin());
    }

    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private ProductResponse mapToResponse(Product product, boolean includeCost) {

        List<String> images = imageUrlUtil.toAbsoluteUrls(product.getImages());

        boolean lowStock = product.getStockQuantity() <= product.getReorderPoint();

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .discountPrice(product.getDiscountPrice())
                .purchasingPrice(includeCost ? product.getPurchasingPrice() : null)
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
                .color(product.getColor())
                .variantGroupId(product.getVariantGroupId())
                .sizes(product.getSizes() != null && !product.getSizes().isEmpty()
                        ? Arrays.stream(product.getSizes().split(","))
                                .filter(s -> !s.trim().isEmpty())
                                .collect(Collectors.toList())
                        : List.of())
                .variants(fetchVariants(product))
                .rating(product.getRating())
                .reviewCount(product.getReviewCount())
                .active(product.getActive())
                .featured(product.getFeatured())
                .reorderPoint(product.getReorderPoint())
                .minStockLevel(product.getMinStockLevel())
                .lowStock(lowStock)
                .build();
    }


    private List<ProductVariantResponse> fetchVariants(Product product) {
        if (product.getVariantGroupId() == null) return List.of();
        return productRepository.findByVariantGroupIdAndActiveTrueAndIdNot(
                product.getVariantGroupId(), product.getId())
                .stream().map(p -> ProductVariantResponse.builder()
                        .id(p.getId())
                        .color(p.getColor())
                        .thumbnail(p.getImages() != null && !p.getImages().isEmpty()
                                ? imageUrlUtil.toAbsoluteUrl(p.getImages().get(0)) : null)
                        .price(p.getPrice())
                        .discountPrice(p.getDiscountPrice())
                        .stockQuantity(p.getStockQuantity())
                        .build())
                .toList();
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

    /** Short code printed on labels and encoded in the QR link: DC + 8 digits. */
    public String generateBarcode() {
        String code;
        do {
            code = String.format("DC%08d", BARCODE_RANDOM.nextInt(100_000_000));
        } while (productRepository.existsByBarcode(code));
        return code;
    }

    /** Resolve a scanned code (our barcode, or a SKU typed by hand). */
    @Transactional(readOnly = true)
    public ProductResponse lookupByCode(String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim();
        if (code.isEmpty()) throw new NotFoundException("Product not found");
        Product product = productRepository.findByBarcode(code)
                .or(() -> productRepository.findByBarcode(code.toUpperCase()))
                .or(() -> productRepository.findBySkuIgnoreCase(code))
                .orElseThrow(() -> new NotFoundException("No product found for code " + code));
        if (!Boolean.TRUE.equals(product.getActive()) && !isCurrentUserAdmin()) {
            throw new NotFoundException("Product not found");
        }
        return mapToResponse(product);
    }

    private static final java.security.SecureRandom BARCODE_RANDOM = new java.security.SecureRandom();

    private static String blankToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private static String joinSizes(List<String> sizes) {
        if (sizes == null) return null;
        String joined = sizes.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(","));
        return joined.isEmpty() ? null : joined;
    }

    private static void validatePrices(BigDecimal price, BigDecimal discountPrice, BigDecimal purchasingPrice) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Price must be greater than 0");
        }
        if (discountPrice != null && (discountPrice.compareTo(BigDecimal.ZERO) <= 0 || discountPrice.compareTo(price) >= 0)) {
            throw new BadRequestException("Discount price must be greater than 0 and less than the price");
        }
        if (purchasingPrice != null && purchasingPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Purchasing price cannot be negative");
        }
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

        String sku;
        do {
            long timestamp = System.currentTimeMillis() / 1000; // Unix timestamp in seconds
            int random = (int) (Math.random() * 1000); // 0-999
            sku = String.format("%s-%d-%03d", prefix, timestamp, random);
        } while (productRepository.existsBySku(sku));
        return sku;
    }

    /**
     * Get low stock products (stock <= reorder point)
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProducts() {
        return productRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
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