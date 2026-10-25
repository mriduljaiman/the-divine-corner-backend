package com.divinecorner.controller;

import com.divinecorner.dto.pos.PosDtos.*;
import com.divinecorner.dto.response.PageResponse;
import com.divinecorner.service.BillingService;
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
 * Counter billing (POS) and customer dues. Admin only.
 */
@RestController
@RequestMapping("/admin/pos")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminPosController {

    private final BillingService billingService;

    @PostMapping("/bills")
    public ResponseEntity<BillResponse> createBill(@AuthenticationPrincipal UUID adminId,
                                                   @Valid @RequestBody CreateBillRequest request) {
        return ResponseEntity.ok(billingService.createBill(adminId, request));
    }

    @GetMapping("/bills")
    public ResponseEntity<PageResponse<BillResponse>> searchBills(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean includeVoided,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(billingService.searchBills(from, to, q, includeVoided, page, size));
    }

    @GetMapping("/bills/{id}")
    public ResponseEntity<BillResponse> getBill(@PathVariable UUID id) {
        return ResponseEntity.ok(billingService.getBill(id));
    }

    @PostMapping("/bills/{id}/payments")
    public ResponseEntity<BillResponse> recordPayment(@AuthenticationPrincipal UUID adminId, @PathVariable UUID id,
                                                      @Valid @RequestBody RecordPaymentRequest request) {
        return ResponseEntity.ok(billingService.recordPayment(adminId, id, request));
    }

    @PostMapping("/bills/{id}/void")
    public ResponseEntity<BillResponse> voidBill(@AuthenticationPrincipal UUID adminId, @PathVariable UUID id,
                                                 @Valid @RequestBody VoidBillRequest request) {
        return ResponseEntity.ok(billingService.voidBill(adminId, id, request));
    }

    @PostMapping("/dues/manual")
    public ResponseEntity<BillResponse> manualDue(@AuthenticationPrincipal UUID adminId,
                                                  @Valid @RequestBody ManualDueRequest request) {
        return ResponseEntity.ok(billingService.createManualDue(adminId, request));
    }

    @PutMapping("/bills/{id}/due-date")
    public ResponseEntity<BillResponse> setDueDate(@PathVariable UUID id, @RequestBody DueDateRequest request) {
        return ResponseEntity.ok(billingService.setDueDate(id, request));
    }

    @GetMapping("/customers/{phone}/ledger")
    public ResponseEntity<LedgerResponse> ledger(@PathVariable String phone) {
        return ResponseEntity.ok(billingService.ledger(phone));
    }

    @PostMapping("/customers/{phone}/reminded")
    public ResponseEntity<Void> reminded(@PathVariable String phone) {
        billingService.markReminded(phone);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dues")
    public ResponseEntity<List<DueCustomerResponse>> dues() {
        return ResponseEntity.ok(billingService.dueCustomers());
    }

    @GetMapping("/customers/{phone}/bills")
    public ResponseEntity<List<BillResponse>> customerBills(@PathVariable String phone) {
        return ResponseEntity.ok(billingService.customerBills(phone));
    }

    @PostMapping("/customers/{phone}/payments")
    public ResponseEntity<List<BillResponse>> collectFromCustomer(@AuthenticationPrincipal UUID adminId, @PathVariable String phone,
                                                                  @Valid @RequestBody RecordPaymentRequest request) {
        return ResponseEntity.ok(billingService.collectFromCustomer(adminId, phone, request));
    }
}
