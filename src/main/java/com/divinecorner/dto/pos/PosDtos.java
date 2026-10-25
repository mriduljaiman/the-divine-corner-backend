package com.divinecorner.dto.pos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request/response shapes for counter billing (POS), dues, expenses and analytics.
 */
public final class PosDtos {

    private PosDtos() {
    }

    // ------------------------------------------------------------------ billing

    public record BillItemRequest(
            @NotNull UUID productId,
            @NotNull @Min(1) Integer quantity,
            /** Price actually charged per unit (after bargaining). */
            @NotNull @DecimalMin(value = "0.01") BigDecimal unitPrice) {
    }

    public record CreateBillRequest(
            @Size(max = 100) String customerName,
            @NotBlank String customerPhone,
            @NotEmpty @Size(max = 200) List<@Valid BillItemRequest> items,
            /** Null = paid in full. */
            @DecimalMin(value = "0.00") BigDecimal amountPaid,
            String paymentMethod,
            @Size(max = 1000) String notes,
            /** Promise date for the due part (optional). */
            LocalDate dueDate) {
    }

    /** An item in a hand-written due entry; productId is optional and never moves stock. */
    public record ManualItemRequest(
            UUID productId,
            @Size(max = 200) String name,
            @NotNull @Min(1) Integer quantity,
            @NotNull @DecimalMin(value = "0.01") BigDecimal unitPrice) {
    }

    /** Record an old / off-system credit sale: who took what, when, total, paid and due. */
    public record ManualDueRequest(
            @Size(max = 100) String customerName,
            @NotBlank String customerPhone,
            LocalDate billDate,
            @NotEmpty @Size(max = 100) List<@Valid ManualItemRequest> items,
            @DecimalMin(value = "0.00") BigDecimal amountPaid,
            String paymentMethod,
            LocalDate dueDate,
            @Size(max = 1000) String notes) {
    }

    public record DueDateRequest(LocalDate dueDate) {
    }

    public record BillItemResponse(
            UUID productId,
            String productName,
            String productSku,
            Integer quantity,
            BigDecimal mrp,
            BigDecimal unitPrice,
            BigDecimal costPrice,
            BigDecimal subtotal) {
    }

    public record PaymentResponse(UUID id, BigDecimal amount, String paymentMethod, String note, String paidAt) {
    }

    public record BillResponse(
            UUID id,
            String billNumber,
            String billDate,
            String customerName,
            String customerPhone,
            List<BillItemResponse> items,
            int totalQuantity,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total,
            BigDecimal amountPaid,
            BigDecimal amountDue,
            String paymentMethod,
            String notes,
            String createdBy,
            boolean voided,
            String voidReason,
            String source,
            LocalDate dueDate,
            BigDecimal writtenOff,
            List<PaymentResponse> payments) {
    }

    public record RecordPaymentRequest(
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            String paymentMethod,
            @Size(max = 500) String note,
            /** Day the money was received; defaults to today, cannot be in the future. */
            LocalDate paidOn,
            /** true = forgive this amount instead of receiving it (closes the account). */
            Boolean writeOff) {
    }

    public record VoidBillRequest(@NotBlank @Size(max = 500) String reason) {
    }

    public record DueCustomerResponse(
            String customerPhone,
            String customerName,
            BigDecimal totalDue,
            long billCount,
            String lastBillDate,
            LocalDate oldestBillDate,
            long daysOutstanding,
            LocalDate nextDueDate,
            /** OVERDUE, DUE_TODAY, STALE (no promise date, older than 30 days), UPCOMING, NO_DATE */
            String status,
            String lastRemindedAt,
            int remindCount) {
    }

    public record LedgerEntry(
            String date,
            /** BILL, PAYMENT, WRITE_OFF */
            String type,
            UUID billId,
            String reference,
            String description,
            BigDecimal debit,
            BigDecimal credit,
            BigDecimal balance,
            String method,
            String note) {
    }

    public record LedgerResponse(
            String customerPhone,
            String customerName,
            BigDecimal totalPurchased,
            BigDecimal totalPaid,
            BigDecimal totalWrittenOff,
            BigDecimal balance,
            boolean settled,
            LocalDate nextDueDate,
            String lastRemindedAt,
            int remindCount,
            List<LedgerEntry> entries) {
    }

    // ------------------------------------------------------------------ expenses

    public record ExpenseRequest(
            LocalDate expenseDate,
            @NotBlank @Size(max = 60) String category,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            String paymentMethod,
            @Size(max = 500) String note) {
    }

    public record ExpenseResponse(
            UUID id,
            LocalDate expenseDate,
            String category,
            BigDecimal amount,
            String paymentMethod,
            String note,
            String createdAt) {
    }

    // ------------------------------------------------------------------ analytics

    public record PeriodStats(
            BigDecimal onlineSales,
            long onlineOrders,
            BigDecimal offlineSales,
            long offlineBills,
            BigDecimal totalSales,
            BigDecimal grossProfit,
            BigDecimal collected,
            BigDecimal expenses,
            BigDecimal purchases,
            BigDecimal netProfit) {
    }

    public record DailyPoint(LocalDate date, BigDecimal online, BigDecimal offline) {
    }

    public record TopProduct(UUID productId, String name, long quantity, BigDecimal revenue) {
    }

    public record NameAmount(String name, BigDecimal amount) {
    }

    public record AnalyticsResponse(
            LocalDate today,
            PeriodStats todayStats,
            PeriodStats monthStats,
            List<DailyPoint> daily,
            BigDecimal outstandingDue,
            long dueCustomers,
            long totalCustomers,
            long newCustomersThisMonth,
            long pendingOrders,
            long lowStockProducts,
            long activeProducts,
            List<TopProduct> topProducts,
            List<NameAmount> expenseByCategory) {
    }
}
