package com.divinecorner.service;

import com.divinecorner.dto.*;
import com.divinecorner.dto.CreateOrderRequest;
import com.divinecorner.dto.UpdateOrderStatusRequest;
import com.divinecorner.dto.response.*;
import com.divinecorner.entity.*;
import com.divinecorner.enums.MovementType;
import com.divinecorner.enums.OrderStatus;
import com.divinecorner.enums.UserRole;
import com.divinecorner.exception.BadRequestException;
import com.divinecorner.exception.NotFoundException;
import com.divinecorner.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import com.divinecorner.utils.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ShippingService shippingService;
    private final EntityManager entityManager;

    private static final DateTimeFormatter ORDER_NO_DATE = DateTimeFormatter.ofPattern("yyMMddHHmmss");
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Transactional
    @CacheEvict(value = {"products", "featuredProducts"}, allEntries = true)
    public OrderResponse createOrder(UUID userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        // Lock every product row (in a stable order to avoid deadlocks) and re-read its stock,
        // so two customers buying the last piece at the same time can't both succeed.
        List<Product> products = cart.getItems().stream()
                .map(CartItem::getProduct)
                .distinct()
                .sorted(Comparator.comparing(p -> p.getId().toString()))
                .toList();
        for (Product product : products) {
            entityManager.refresh(product, LockModeType.PESSIMISTIC_WRITE);
        }

        Map<UUID, Integer> requestedQty = new HashMap<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new BadRequestException(product.getName() + " is no longer available. Please remove it from your cart");
            }
            int totalForProduct = requestedQty.merge(product.getId(), cartItem.getQuantity(), Integer::sum);
            if (product.getStockQuantity() < totalForProduct) {
                throw new BadRequestException("Insufficient stock for " + product.getName()
                        + " (only " + product.getStockQuantity() + " left)");
            }
            subtotal = subtotal.add(unitPrice(product).multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        BigDecimal shippingCharges = shippingService.calculateShippingCharges(subtotal, null);
        BigDecimal totalAmount = subtotal.add(shippingCharges);

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUser(user);
        order.setSubtotal(subtotal);
        order.setShippingCharges(shippingCharges);
        order.setDiscount(BigDecimal.ZERO);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(request.getShippingAddress().trim());
        order.setShippingCity(request.getShippingCity().trim());
        order.setShippingState(request.getShippingState().trim());
        order.setShippingZipCode(request.getShippingZipCode().trim());
        order.setShippingCountry(request.getShippingCountry().trim());
        order.setCustomerPhone(request.getCustomerPhone().trim());
        order.setCustomerEmail(request.getCustomerEmail().trim());
        // Only Cash on Delivery is supported for now
        order.setPaymentMethod("COD");
        order.setPaymentStatus("PENDING");

        order = orderRepository.save(order);
        orderRepository.flush(); // Ensure timestamps are populated

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            BigDecimal price = unitPrice(product);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .size(cartItem.getSize())
                    .price(price)
                    .productName(product.getName())
                    .productImage(product.getImages() != null && !product.getImages().isEmpty() ?
                            product.getImages().get(0) : null)
                    .build();

            order.getItems().add(orderItem);
            orderItemRepository.save(orderItem);

            int newBalance = product.getStockQuantity() - cartItem.getQuantity();
            product.setStockQuantity(newBalance);
            productRepository.save(product);

            StockMovement movement = StockMovement.builder()
                .product(product)
                .movementType(MovementType.ONLINE_ORDER)
                .quantity(-cartItem.getQuantity())  // Negative for stock OUT
                .balanceAfter(newBalance)
                .movementDate(LocalDateTime.now())
                .referenceType("ORDER")
                .referenceId(order.getId().toString())
                .createdBy(user)
                .unitCost(price)  // Using the sale price
                .totalValue(price.multiply(new BigDecimal(cartItem.getQuantity())))
                .remarks("Online order: " + order.getOrderNumber())
                .build();

            stockMovementRepository.save(movement);
        }

        cartService.clearCart(userId);

        return mapToResponse(order);
    }

    private static BigDecimal unitPrice(Product product) {
        return product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getPrice();
    }

    private String generateOrderNumber() {
        String orderNumber;
        do {
            orderNumber = "ORD-" + LocalDateTime.now().format(ORDER_NO_DATE)
                    + "-" + String.format("%04d", RANDOM.nextInt(10000));
        } while (orderRepository.existsByOrderNumber(orderNumber));
        return orderNumber;
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(UUID userId, Pageable pageable) {
        Page<Order> page = orderRepository.findByUserId(userId, pageable);
        return mapToPageResponse(page);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID userId, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new NotFoundException("Order not found");
        }

        return mapToResponse(order);
    }

    /** Customers may cancel their own order until it is being processed. */
    @Transactional
    @CacheEvict(value = {"products", "featuredProducts"}, allEntries = true)
    public OrderResponse cancelMyOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new NotFoundException("Order not found");
        }
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException("This order can no longer be cancelled");
        }

        restockCancelledOrder(order);
        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus("CANCELLED");
        return mapToResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(String status, Pageable pageable) {
        Page<Order> page;
        if (status == null || status.isBlank()) {
            page = orderRepository.findAll(pageable);
        } else {
            try {
                page = orderRepository.findByStatus(OrderStatus.valueOf(status.toUpperCase()), pageable);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid order status");
            }
        }
        return mapToPageResponse(page);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByIdAdmin(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        return mapToResponse(order);
    }

    @Transactional
    @CacheEvict(value = {"products", "featuredProducts"}, allEntries = true)
    public OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        OrderStatus status;
        try {
            status = OrderStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid order status");
        }

        if (order.getStatus() == OrderStatus.CANCELLED && status != OrderStatus.CANCELLED) {
            throw new BadRequestException("Cancelled order cannot be reopened");
        }

        if (order.getStatus() == OrderStatus.DELIVERED && status != OrderStatus.DELIVERED) {
            throw new BadRequestException("Delivered order status cannot be changed");
        }

        if (status == OrderStatus.CANCELLED && order.getStatus() != OrderStatus.CANCELLED) {
            restockCancelledOrder(order);
            order.setPaymentStatus("CANCELLED");
        }

        if (status == OrderStatus.DELIVERED && order.getStatus() != OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
            order.setPaymentStatus("COMPLETED");
        }

        order.setStatus(status);

        if (request.getTrackingNumber() != null) {
            String tracking = request.getTrackingNumber().trim();
            order.setTrackingNumber(tracking.isEmpty() ? null : tracking);
        }

        order = orderRepository.save(order);
        return mapToResponse(order);
    }

    private void restockCancelledOrder(Order order) {
        order.getItems().stream()
                .map(OrderItem::getProduct)
                .distinct()
                .sorted(Comparator.comparing(p -> p.getId().toString()))
                .forEach(p -> entityManager.refresh(p, LockModeType.PESSIMISTIC_WRITE));

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            int newBalance = product.getStockQuantity() + item.getQuantity();
            product.setStockQuantity(newBalance);
            productRepository.save(product);

            stockMovementRepository.save(StockMovement.builder()
                    .product(product)
                    .movementType(MovementType.ADJUSTMENT)
                    .quantity(item.getQuantity())
                    .balanceAfter(newBalance)
                    .movementDate(LocalDateTime.now())
                    .referenceType("ORDER_CANCEL")
                    .referenceId(order.getId().toString())
                    .unitCost(item.getPrice())
                    .totalValue(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .remarks("Order cancelled: " + order.getOrderNumber())
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        Long totalOrders = orderRepository.count();
        Long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        Long deliveredOrders = orderRepository.countByStatus(OrderStatus.DELIVERED);
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue();

        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
        LocalDateTime endOfMonth = LocalDateTime.now();
        BigDecimal monthlyRevenue = orderRepository.calculateRevenueByDateRange(startOfMonth, endOfMonth);

        Long totalProducts = productRepository.countByActiveTrue();
        Long lowStockProducts = (long) productRepository.findLowStockProducts().size();
        Long totalUsers = userRepository.countByRole(UserRole.USER);

        List<RecentOrderResponse> recentOrders = orderRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(order -> RecentOrderResponse.builder()
                        .id(order.getId())
                        .orderNumber(order.getOrderNumber())
                        .customerName((nullToEmpty(order.getUser().getFirstName()) + " " + nullToEmpty(order.getUser().getLastName())).trim())
                        .totalAmount(order.getTotalAmount())
                        .status(order.getStatus().name())
                        .createdAt(TimeUtil.format(order.getCreatedAt()))
                        .build())
                .toList();

        return DashboardResponse.builder()
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .deliveredOrders(deliveredOrders)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .monthlyRevenue(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO)
                .totalProducts(totalProducts)
                .lowStockProducts(lowStockProducts)
                .totalUsers(totalUsers)
                .recentOrders(recentOrders)
                .build();
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerName(order.getUser() != null ?
                        (nullToEmpty(order.getUser().getFirstName()) + " " + nullToEmpty(order.getUser().getLastName())).trim() : null)
                .items(order.getItems().stream().map(this::mapToItemResponse).toList())
                .subtotal(order.getSubtotal() != null ? order.getSubtotal() : BigDecimal.ZERO)
                .shippingCharges(order.getShippingCharges() != null ? order.getShippingCharges() : BigDecimal.ZERO)
                .discount(order.getDiscount() != null ? order.getDiscount() : BigDecimal.ZERO)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .shippingAddress(order.getShippingAddress())
                .shippingCity(order.getShippingCity())
                .shippingState(order.getShippingState())
                .shippingZipCode(order.getShippingZipCode())
                .shippingCountry(order.getShippingCountry())
                .customerPhone(order.getCustomerPhone())
                .customerEmail(order.getCustomerEmail())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .trackingNumber(order.getTrackingNumber())
                .createdAt(order.getCreatedAt() != null ? TimeUtil.format(order.getCreatedAt()) : TimeUtil.format(LocalDateTime.now()))
                .deliveredAt(TimeUtil.format(order.getDeliveredAt()))
                .build();
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    private OrderItemResponse mapToItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProductName())
                .productImage(item.getProductImage())
                .quantity(item.getQuantity())
                .size(item.getSize())
                .price(item.getPrice())
                .subtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .build();
    }

    private PageResponse<OrderResponse> mapToPageResponse(Page<Order> page) {
        return PageResponse.<OrderResponse>builder()
                .content(page.getContent().stream().map(this::mapToResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}