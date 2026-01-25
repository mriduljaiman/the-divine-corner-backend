package com.divinecorner.controller;

import com.divinecorner.dto.request.StockInRequest;
import com.divinecorner.dto.response.StockInResponse;
import com.divinecorner.service.StockInService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stock IN (Goods Inward) API endpoints
 * Admin-only access for recording incoming stock
 */
@RestController
@RequestMapping("/api/stock-in")
@RequiredArgsConstructor
public class StockInController {

    private final StockInService stockInService;

    /**
     * Record a new stock IN entry
     * POST /api/stock-in
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StockInResponse> recordStockIn(@Valid @RequestBody StockInRequest request) {
        StockInResponse response = stockInService.recordStockIn(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get stock IN history with optional filters
     * GET /api/stock-in/history
     * Query params:
     * - productId (optional): Filter by product
     * - startDate (optional): Start date (ISO format)
     * - endDate (optional): End date (ISO format)
     * - page (default: 0)
     * - size (default: 20)
     * - sort (default: movementDate,desc)
     */
    @GetMapping("/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<StockInResponse>> getStockInHistory(
        @RequestParam(required = false) UUID productId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "movementDate,desc") String sort
    ) {
        // Parse sort parameter
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        // Set default date range if not provided (last 30 days)
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        Page<StockInResponse> history = stockInService.getStockInHistory(productId, startDate, endDate, pageable);
        return ResponseEntity.ok(history);
    }

    /**
     * Get a single stock IN entry by ID
     * GET /api/stock-in/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StockInResponse> getStockInById(@PathVariable UUID id) {
        StockInResponse response = stockInService.getStockInById(id);
        return ResponseEntity.ok(response);
    }
}
