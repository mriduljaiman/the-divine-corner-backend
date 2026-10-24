package com.divinecorner.service;

import com.divinecorner.dto.pos.PosDtos.*;
import com.divinecorner.enums.OrderStatus;
import com.divinecorner.enums.UserRole;
import com.divinecorner.repository.*;
import com.divinecorner.utils.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Admin dashboard numbers. Every sum is computed in the database; only the 30-day series
 * is bucketed in memory (a few hundred rows at most).
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final int SERIES_DAYS = 30;
    private static final int TOP_PRODUCTS = 5;

    private final OrderRepository orderRepository;
    private final OfflineBillRepository billRepository;
    private final DuePaymentRepository duePaymentRepository;
    private final ExpenseRepository expenseRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public AnalyticsResponse summary() {
        LocalDate today = TimeUtil.shopToday();
        LocalDate monthStart = today.withDayOfMonth(1);

        PeriodStats todayStats = period(today, today);
        PeriodStats monthStats = period(monthStart, today);

        List<NameAmount> expenseByCategory = expenseRepository.byCategory(monthStart, today).stream()
                .map(r -> new NameAmount((String) r[0], (BigDecimal) r[1]))
                .toList();

        return new AnalyticsResponse(
                today,
                todayStats,
                monthStats,
                dailySeries(today.minusDays(SERIES_DAYS - 1), today),
                billRepository.totalOutstandingDue(),
                billRepository.findDueSummary().size(),
                userRepository.countByRole(UserRole.USER),
                userRepository.countByRoleAndCreatedAtGreaterThanEqual(UserRole.USER, TimeUtil.startOfShopDay(monthStart)),
                orderRepository.countByStatus(OrderStatus.PENDING) + orderRepository.countByStatus(OrderStatus.CONFIRMED),
                productRepository.findLowStockProducts().size(),
                productRepository.countByActiveTrue(),
                topProducts(monthStart, today),
                expenseByCategory);
    }

    /** Stats for shop-local days [from, to] inclusive. */
    private PeriodStats period(LocalDate from, LocalDate to) {
        LocalDateTime start = TimeUtil.startOfShopDay(from);
        LocalDateTime end = TimeUtil.startOfShopDay(to.plusDays(1));

        Object[] online = orderRepository.bookedTotals(start, end).get(0);
        Object[] offline = billRepository.totals(start, end).get(0);
        BigDecimal onlineSales = (BigDecimal) online[0];
        BigDecimal offlineSales = (BigDecimal) offline[0];
        BigDecimal gross = orderRepository.bookedGrossProfit(start, end).add(billRepository.grossProfit(start, end));
        BigDecimal collected = ((BigDecimal) offline[1]).add(duePaymentRepository.collectedBetween(start, end));
        BigDecimal expenses = expenseRepository.total(from, to);
        BigDecimal purchases = stockMovementRepository.purchaseValue(start, end);

        return new PeriodStats(
                onlineSales,
                ((Number) online[1]).longValue(),
                offlineSales,
                ((Number) offline[2]).longValue(),
                onlineSales.add(offlineSales),
                gross,
                collected,
                expenses,
                purchases,
                gross.subtract(expenses));
    }

    private List<DailyPoint> dailySeries(LocalDate from, LocalDate to) {
        LocalDateTime start = TimeUtil.startOfShopDay(from);
        LocalDateTime end = TimeUtil.startOfShopDay(to.plusDays(1));
        // TreeMap keeps days ordered; pre-fill so days without sales show as zero
        TreeMap<LocalDate, BigDecimal[]> buckets = new TreeMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            buckets.put(d, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        for (Object[] r : orderRepository.bookedDailyRows(start, end)) {
            BigDecimal[] b = buckets.get(TimeUtil.toShopDate((LocalDateTime) r[0]));
            if (b != null) b[0] = b[0].add((BigDecimal) r[1]);
        }
        for (Object[] r : billRepository.dailyRows(start, end)) {
            BigDecimal[] b = buckets.get(TimeUtil.toShopDate((LocalDateTime) r[0]));
            if (b != null) b[1] = b[1].add((BigDecimal) r[1]);
        }
        List<DailyPoint> out = new ArrayList<>(buckets.size());
        buckets.forEach((d, v) -> out.add(new DailyPoint(d, v[0], v[1])));
        return out;
    }

    private List<TopProduct> topProducts(LocalDate from, LocalDate to) {
        LocalDateTime start = TimeUtil.startOfShopDay(from);
        LocalDateTime end = TimeUtil.startOfShopDay(to.plusDays(1));
        Map<UUID, TopProduct> merged = new HashMap<>();
        List<Object[]> rows = new ArrayList<>(orderRepository.productSales(start, end));
        rows.addAll(billRepository.productSales(start, end));
        for (Object[] r : rows) {
            UUID id = (UUID) r[0];
            long qty = ((Number) r[2]).longValue();
            BigDecimal revenue = (BigDecimal) r[3];
            merged.merge(id, new TopProduct(id, (String) r[1], qty, revenue),
                    (a, b) -> new TopProduct(id, a.name(), a.quantity() + b.quantity(), a.revenue().add(b.revenue())));
        }
        // Min-heap of size K: O(n log k) instead of sorting everything
        PriorityQueue<TopProduct> heap = new PriorityQueue<>(Comparator.comparing(TopProduct::revenue));
        for (TopProduct p : merged.values()) {
            heap.offer(p);
            if (heap.size() > TOP_PRODUCTS) heap.poll();
        }
        List<TopProduct> top = new ArrayList<>(heap);
        top.sort(Comparator.comparing(TopProduct::revenue).reversed());
        return top;
    }
}
