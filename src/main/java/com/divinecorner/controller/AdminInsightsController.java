package com.divinecorner.controller;

import com.divinecorner.dto.pos.PosDtos.AnalyticsResponse;
import com.divinecorner.dto.pos.PosDtos.ExpenseRequest;
import com.divinecorner.dto.pos.PosDtos.ExpenseResponse;
import com.divinecorner.dto.response.ApiResponse;
import com.divinecorner.service.AnalyticsService;
import com.divinecorner.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Admin analytics dashboard and expense management.
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminInsightsController {

    private final AnalyticsService analyticsService;
    private final ExpenseService expenseService;

    @GetMapping("/analytics")
    public ResponseEntity<AnalyticsResponse> analytics() {
        return ResponseEntity.ok(analyticsService.summary());
    }

    @GetMapping("/expenses")
    public ResponseEntity<List<ExpenseResponse>> expenses(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(expenseService.list(from, to));
    }

    @PostMapping("/expenses")
    public ResponseEntity<ExpenseResponse> createExpense(@AuthenticationPrincipal UUID adminId,
                                                         @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(expenseService.create(adminId, request));
    }

    @PutMapping("/expenses/{id}")
    public ResponseEntity<ExpenseResponse> updateExpense(@PathVariable UUID id, @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(expenseService.update(id, request));
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<ApiResponse> deleteExpense(@PathVariable UUID id) {
        expenseService.delete(id);
        return ResponseEntity.ok(ApiResponse.builder().success(true).message("Expense deleted").build());
    }
}
