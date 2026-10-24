package com.divinecorner.service;

import com.divinecorner.dto.pos.PosDtos.*;
import com.divinecorner.dto.response.PageResponse;
import com.divinecorner.entity.*;
import com.divinecorner.enums.MovementType;
import com.divinecorner.exception.BadRequestException;
import com.divinecorner.exception.NotFoundException;
import com.divinecorner.repository.*;
import com.divinecorner.utils.TimeUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Counter billing (POS): scanned items, bargained prices, partial payments and dues.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

    private static final Pattern INDIAN_MOBILE = Pattern.compile("^[6-9]\\d{9}$");
    private static final Set<String> PAYMENT_METHODS = Set.of("CASH", "UPI", "CARD", "MIXED", "DUE");
    private static final DateTimeFormatter BILL_NO_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OfflineBillRepository billRepository;
    private final DuePaymentRepository duePaymentRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    /** Accepts "98765 43210", "+91-9876543210", "09876543210" ... and returns the 10 digit number. */
    public static String normalizePhone(String raw) {
        if (raw == null) {
            throw new BadRequestException("Customer mobile number is required");
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) digits = digits.substring(2);
        if (digits.length() == 11 && digits.startsWith("0")) digits = digits.substring(1);
        if (!INDIAN_MOBILE.matcher(digits).matches()) {
            throw new BadRequestException("Enter a valid 10-digit mobile number");
        }
        return digits;
    }

    @Transactional
    @CacheEvict(value = {"products", "featuredProducts"}, allEntries = true)
    public BillResponse createBill(UUID adminId, CreateBillRequest request) {
        String phone = normalizePhone(request.customerPhone());
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Lock each product once, in a stable order, then validate stock for the whole bill
        Map<UUID, Integer> qtyByProduct = new HashMap<>();
        for (BillItemRequest item : request.items()) {
            qtyByProduct.merge(item.productId(), item.quantity(), Integer::sum);
        }
        Map<UUID, Product> products = new HashMap<>();
        for (UUID id : new TreeSet<>(qtyByProduct.keySet())) {
            Product product = entityManager.find(Product.class, id, LockModeType.PESSIMISTIC_WRITE);
            if (product == null) {
                throw new NotFoundException("Product not found");
            }
            entityManager.refresh(product, LockModeType.PESSIMISTIC_WRITE);
            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new BadRequestException(product.getName() + " is no longer active");
            }
            int wanted = qtyByProduct.get(id);
            if (product.getStockQuantity() < wanted) {
                throw new BadRequestException("Only " + product.getStockQuantity() + " in stock for " + product.getName());
            }
            products.put(id, product);
        }

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal mrpTotal = BigDecimal.ZERO;
        for (BillItemRequest item : request.items()) {
            Product product = products.get(item.productId());
            BigDecimal qty = BigDecimal.valueOf(item.quantity());
            total = total.add(money(item.unitPrice()).multiply(qty));
            mrpTotal = mrpTotal.add(listedPrice(product).multiply(qty));
        }
        total = money(total);

        BigDecimal paid = request.amountPaid() == null ? total : money(request.amountPaid());
        if (paid.compareTo(total) > 0) {
            throw new BadRequestException("Paid amount cannot be more than the bill total (₹" + total + ")");
        }
        BigDecimal due = total.subtract(paid);

        String method = request.paymentMethod() == null || request.paymentMethod().isBlank()
                ? "CASH" : request.paymentMethod().trim().toUpperCase();
        if (!PAYMENT_METHODS.contains(method)) {
            throw new BadRequestException("Invalid payment method");
        }
        if (paid.signum() == 0) method = "DUE";

        BigDecimal discount = mrpTotal.subtract(total).max(BigDecimal.ZERO);

        OfflineBill bill = OfflineBill.builder()
                .billNumber(generateBillNumber())
                .billDate(LocalDateTime.now())
                .createdBy(admin)
                .customerName(blankToNull(request.customerName()))
                .customerPhone(phone)
                .subtotal(money(total.add(discount)))
                .discount(money(discount))
                .tax(BigDecimal.ZERO)
                .total(total)
                .amountPaid(paid)
                .paidAtBilling(paid)
                .amountDue(due)
                .paymentMethod(method)
                .notes(blankToNull(request.notes()))
                .voided(false)
                .build();
        bill = billRepository.save(bill);

        for (BillItemRequest item : request.items()) {
            Product product = products.get(item.productId());
            BigDecimal unitPrice = money(item.unitPrice());
            BigDecimal lineTotal = money(unitPrice.multiply(BigDecimal.valueOf(item.quantity())));
            BigDecimal mrp = listedPrice(product);

            bill.getItems().add(OfflineBillItem.builder()
                    .bill(bill)
                    .product(product)
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .quantity(item.quantity())
                    .unitPrice(unitPrice)
                    .mrp(mrp)
                    .costPrice(product.getPurchasingPrice())
                    .discount(money(mrp.subtract(unitPrice).max(BigDecimal.ZERO).multiply(BigDecimal.valueOf(item.quantity()))))
                    .subtotal(lineTotal)
                    .build());

            int newBalance = product.getStockQuantity() - item.quantity();
            product.setStockQuantity(newBalance);

            stockMovementRepository.save(StockMovement.builder()
                    .product(product)
                    .movementType(MovementType.OFFLINE_SALE)
                    .quantity(-item.quantity())
                    .balanceAfter(newBalance)
                    .movementDate(LocalDateTime.now())
                    .referenceType("OFFLINE_BILL")
                    .referenceId(bill.getId().toString())
                    .createdBy(admin)
                    .unitCost(unitPrice)
                    .totalValue(lineTotal)
                    .remarks("Counter bill " + bill.getBillNumber())
                    .build());
        }
        bill = billRepository.save(bill);
        log.info("Bill {} created: total={}, paid={}, due={}", bill.getBillNumber(), total, paid, due);
        return toResponse(bill, List.of());
    }

    @Transactional(readOnly = true)
    public PageResponse<BillResponse> searchBills(LocalDate from, LocalDate to, String q, boolean includeVoided, int page, int size) {
        LocalDate start = from != null ? from : TimeUtil.shopToday().minusDays(30);
        LocalDate end = to != null ? to : TimeUtil.shopToday();
        if (end.isBefore(start)) {
            throw new BadRequestException("'to' date must be after 'from' date");
        }
        String term = q == null || q.isBlank() ? null : q.trim();
        Page<OfflineBill> result = billRepository.search(TimeUtil.startOfShopDay(start), TimeUtil.startOfShopDay(end.plusDays(1)),
                term, includeVoided, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("billDate").descending()));
        return PageResponse.<BillResponse>builder()
                .content(result.getContent().stream().map(b -> toResponse(b, null)).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public BillResponse getBill(UUID id) {
        OfflineBill bill = findBill(id);
        return toResponse(bill, duePaymentRepository.findByBillIdOrderByPaidAtAsc(id));
    }

    @Transactional
    public BillResponse recordPayment(UUID adminId, UUID billId, RecordPaymentRequest request) {
        OfflineBill bill = findBill(billId);
        entityManager.refresh(bill, LockModeType.PESSIMISTIC_WRITE);
        if (Boolean.TRUE.equals(bill.getVoided())) {
            throw new BadRequestException("Bill is void");
        }
        applyPayment(bill, money(request.amount()), request.paymentMethod(), request.note(), adminId, true);
        return toResponse(bill, duePaymentRepository.findByBillIdOrderByPaidAtAsc(billId));
    }

    /** Collect a payment from a customer and settle their oldest dues first (FIFO). */
    @Transactional
    public List<BillResponse> collectFromCustomer(UUID adminId, String rawPhone, RecordPaymentRequest request) {
        String phone = normalizePhone(rawPhone);
        BigDecimal remaining = money(request.amount());
        List<OfflineBill> open = billRepository.findByCustomerPhoneAndVoidedFalseAndAmountDueGreaterThanOrderByBillDateAsc(phone, BigDecimal.ZERO);
        BigDecimal totalDue = open.stream().map(OfflineBill::getAmountDue).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (open.isEmpty()) {
            throw new BadRequestException("No pending due for this customer");
        }
        if (remaining.compareTo(totalDue) > 0) {
            throw new BadRequestException("Amount is more than the total due (₹" + totalDue + ")");
        }
        List<BillResponse> touched = new ArrayList<>();
        for (OfflineBill bill : open) {
            if (remaining.signum() <= 0) break;
            entityManager.refresh(bill, LockModeType.PESSIMISTIC_WRITE);
            BigDecimal part = remaining.min(bill.getAmountDue());
            if (part.signum() <= 0) continue;
            applyPayment(bill, part, request.paymentMethod(), request.note(), adminId, false);
            remaining = remaining.subtract(part);
            touched.add(toResponse(bill, null));
        }
        return touched;
    }

    @Transactional
    @CacheEvict(value = {"products", "featuredProducts"}, allEntries = true)
    public BillResponse voidBill(UUID adminId, UUID billId, VoidBillRequest request) {
        OfflineBill bill = findBill(billId);
        entityManager.refresh(bill, LockModeType.PESSIMISTIC_WRITE);
        if (Boolean.TRUE.equals(bill.getVoided())) {
            throw new BadRequestException("Bill is already void");
        }
        User admin = userRepository.findById(adminId).orElse(null);
        bill.getItems().stream()
                .map(OfflineBillItem::getProduct)
                .distinct()
                .sorted(Comparator.comparing(p -> p.getId().toString()))
                .forEach(p -> entityManager.refresh(p, LockModeType.PESSIMISTIC_WRITE));
        for (OfflineBillItem item : bill.getItems()) {
            Product product = item.getProduct();
            int newBalance = product.getStockQuantity() + item.getQuantity();
            product.setStockQuantity(newBalance);
            stockMovementRepository.save(StockMovement.builder()
                    .product(product)
                    .movementType(MovementType.ADJUSTMENT)
                    .quantity(item.getQuantity())
                    .balanceAfter(newBalance)
                    .movementDate(LocalDateTime.now())
                    .referenceType("OFFLINE_BILL_VOID")
                    .referenceId(bill.getId().toString())
                    .createdBy(admin)
                    .unitCost(item.getUnitPrice())
                    .totalValue(item.getSubtotal())
                    .remarks("Bill voided: " + bill.getBillNumber())
                    .build());
        }
        bill.setVoided(true);
        bill.setVoidedAt(LocalDateTime.now());
        bill.setVoidReason(request.reason().trim());
        bill.setAmountDue(BigDecimal.ZERO);
        return toResponse(billRepository.save(bill), duePaymentRepository.findByBillIdOrderByPaidAtAsc(billId));
    }

    @Transactional(readOnly = true)
    public List<DueCustomerResponse> dueCustomers() {
        return billRepository.findDueSummary().stream()
                .map(r -> new DueCustomerResponse(
                        (String) r[0],
                        (String) r[1],
                        money((BigDecimal) r[2]),
                        ((Number) r[3]).longValue(),
                        TimeUtil.format((LocalDateTime) r[4])))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BillResponse> customerBills(String rawPhone) {
        return billRepository.findByCustomerPhoneOrderByBillDateDesc(normalizePhone(rawPhone)).stream()
                .map(b -> toResponse(b, null))
                .toList();
    }

    // ------------------------------------------------------------------ helpers

    private void applyPayment(OfflineBill bill, BigDecimal amount, String method, String note, UUID adminId, boolean strict) {
        if (amount.signum() <= 0) {
            throw new BadRequestException("Amount must be greater than 0");
        }
        if (amount.compareTo(bill.getAmountDue()) > 0) {
            if (strict) throw new BadRequestException("Amount is more than the due (₹" + bill.getAmountDue() + ")");
            amount = bill.getAmountDue();
        }
        String pm = method == null || method.isBlank() ? "CASH" : method.trim().toUpperCase();
        if (!PAYMENT_METHODS.contains(pm) || "DUE".equals(pm)) {
            throw new BadRequestException("Invalid payment method");
        }
        bill.setAmountPaid(bill.getAmountPaid().add(amount));
        bill.setAmountDue(bill.getAmountDue().subtract(amount));
        billRepository.save(bill);
        duePaymentRepository.save(DuePayment.builder()
                .bill(bill)
                .amount(amount)
                .paymentMethod(pm)
                .note(blankToNull(note))
                .receivedBy(adminId != null ? userRepository.findById(adminId).orElse(null) : null)
                .build());
    }

    private OfflineBill findBill(UUID id) {
        return billRepository.findById(id).orElseThrow(() -> new NotFoundException("Bill not found"));
    }

    private String generateBillNumber() {
        String number;
        do {
            number = "DC-" + TimeUtil.shopToday().format(BILL_NO_DATE) + "-" + String.format("%04d", RANDOM.nextInt(10000));
        } while (billRepository.existsByBillNumber(number));
        return number;
    }

    private static BigDecimal listedPrice(Product p) {
        return money(p.getDiscountPrice() != null ? p.getDiscountPrice() : p.getPrice());
    }

    private static BigDecimal money(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.setScale(2, RoundingMode.HALF_UP);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private BillResponse toResponse(OfflineBill b, List<DuePayment> payments) {
        List<BillItemResponse> items = b.getItems().stream()
                .map(i -> new BillItemResponse(i.getProduct().getId(), i.getProductName(), i.getProductSku(), i.getQuantity(),
                        i.getMrp(), i.getUnitPrice(), i.getCostPrice(), i.getSubtotal()))
                .toList();
        int qty = b.getItems().stream().mapToInt(OfflineBillItem::getQuantity).sum();
        List<PaymentResponse> paymentList = payments == null ? null : payments.stream()
                .map(p -> new PaymentResponse(p.getId(), p.getAmount(), p.getPaymentMethod(), p.getNote(), TimeUtil.format(p.getPaidAt())))
                .toList();
        String createdBy = b.getCreatedBy() != null
                ? (Objects.toString(b.getCreatedBy().getFirstName(), "") + " " + Objects.toString(b.getCreatedBy().getLastName(), "")).trim()
                : null;
        return new BillResponse(b.getId(), b.getBillNumber(), TimeUtil.format(b.getBillDate()), b.getCustomerName(), b.getCustomerPhone(),
                items, qty, b.getSubtotal(), b.getDiscount(), b.getTotal(), b.getAmountPaid(), b.getAmountDue(), b.getPaymentMethod(),
                b.getNotes(), createdBy, Boolean.TRUE.equals(b.getVoided()), b.getVoidReason(), paymentList);
    }
}
