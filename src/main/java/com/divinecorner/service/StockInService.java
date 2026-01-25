package com.divinecorner.service;

import com.divinecorner.dto.request.StockInRequest;
import com.divinecorner.dto.response.StockInResponse;
import com.divinecorner.entity.Product;
import com.divinecorner.entity.StockMovement;
import com.divinecorner.entity.Supplier;
import com.divinecorner.entity.User;
import com.divinecorner.enums.MovementType;
import com.divinecorner.exception.ResourceNotFoundException;
import com.divinecorner.repository.ProductRepository;
import com.divinecorner.repository.StockMovementRepository;
import com.divinecorner.repository.SupplierRepository;
import com.divinecorner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockInService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;

    /**
     * Record stock IN (Goods Inward)
     * Creates a StockMovement entry and updates product inventory
     */
    @Transactional
    public StockInResponse recordStockIn(StockInRequest request) {
        log.info("Recording stock IN for product: {}, quantity: {}", request.getProductId(), request.getQuantity());

        // 1. Get the product
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        // 2. Get current stock balance
        Integer currentStock = stockMovementRepository.getCurrentStock(request.getProductId());
        if (currentStock == null) {
            currentStock = 0;
        }

        // 3. Calculate new balance
        Integer newBalance = currentStock + request.getQuantity();

        // 4. Get supplier (optional)
        Supplier supplier = null;
        if (request.getSupplierId() != null) {
            supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + request.getSupplierId()));
        }

        // 5. Get current admin user
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User admin = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        // 6. Calculate total value if unit cost provided
        BigDecimal totalValue = null;
        if (request.getUnitCost() != null) {
            totalValue = request.getUnitCost().multiply(new BigDecimal(request.getQuantity()));
        }

        // 7. Create stock movement entry
        StockMovement movement = StockMovement.builder()
            .product(product)
            .movementType(MovementType.STOCK_IN)
            .quantity(request.getQuantity())  // Positive for stock IN
            .balanceAfter(newBalance)
            .movementDate(request.getMovementDate() != null ? request.getMovementDate() : LocalDateTime.now())
            .referenceType("STOCK_IN")
            .supplier(supplier)
            .createdBy(admin)
            .unitCost(request.getUnitCost())
            .totalValue(totalValue)
            .remarks(request.getRemarks())
            .invoiceNumber(request.getInvoiceNumber())
            .build();

        movement = stockMovementRepository.save(movement);
        stockMovementRepository.flush();

        log.info("Stock IN recorded successfully. Movement ID: {}, New balance: {}", movement.getId(), newBalance);

        // 8. Map to response
        return mapToResponse(movement);
    }

    /**
     * Get stock IN history with filters
     */
    public Page<StockInResponse> getStockInHistory(
        UUID productId,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Pageable pageable
    ) {
        Page<StockMovement> movements;

        if (productId != null) {
            movements = stockMovementRepository.findByProductIdAndMovementTypeAndMovementDateBetween(
                productId, MovementType.STOCK_IN, startDate, endDate, pageable
            );
        } else {
            movements = stockMovementRepository.findByMovementTypeAndMovementDateBetween(
                MovementType.STOCK_IN, startDate, endDate, pageable
            );
        }

        return movements.map(this::mapToResponse);
    }

    /**
     * Get single stock IN entry by ID
     */
    public StockInResponse getStockInById(UUID id) {
        StockMovement movement = stockMovementRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Stock IN entry not found with id: " + id));

        if (!movement.getMovementType().equals(MovementType.STOCK_IN)) {
            throw new IllegalArgumentException("Movement is not a STOCK_IN entry");
        }

        return mapToResponse(movement);
    }

    /**
     * Map StockMovement entity to StockInResponse DTO
     */
    private StockInResponse mapToResponse(StockMovement movement) {
        return StockInResponse.builder()
            .id(movement.getId())
            .productId(movement.getProduct().getId())
            .productName(movement.getProduct().getName())
            .productSku(movement.getProduct().getSku())
            .quantity(movement.getQuantity())
            .balanceAfter(movement.getBalanceAfter())
            .movementDate(movement.getMovementDate())
            .supplierId(movement.getSupplier() != null ? movement.getSupplier().getId() : null)
            .supplierName(movement.getSupplier() != null ? movement.getSupplier().getName() : null)
            .invoiceNumber(movement.getInvoiceNumber())
            .unitCost(movement.getUnitCost())
            .totalValue(movement.getTotalValue())
            .remarks(movement.getRemarks())
            .createdByName(movement.getCreatedBy() != null ? movement.getCreatedBy().getEmail() : null)
            .createdAt(movement.getCreatedAt())
            .build();
    }
}
