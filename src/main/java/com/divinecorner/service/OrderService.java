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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
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

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional
    public OrderResponse createOrder(UUID userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            if (cartItem.getProduct().getStockQuantity() < cartItem.getQuantity()) {
                throw new BadRequestException("Insufficient stock for " + cartItem.getProduct().getName());
            }
            BigDecimal price = cartItem.getProduct().getDiscountPrice() != null ?
                    cartItem.getProduct().getDiscountPrice() : cartItem.getProduct().getPrice();
            subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        // Calculate shipping charges (can inject ShippingService later)
        BigDecimal shippingCharges = subtotal.compareTo(new BigDecimal("500")) >= 0 ?
            BigDecimal.ZERO : new BigDecimal("50");

        BigDecimal totalAmount = subtotal.add(shippingCharges);

        String orderNumber = "ORD-" + System.currentTimeMillis();

        // Create order entity without using builder to avoid timestamp issues
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUser(user);
        order.setSubtotal(subtotal);
        order.setShippingCharges(shippingCharges);
        order.setDiscount(BigDecimal.ZERO);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(request.getShippingAddress());
        order.setShippingCity(request.getShippingCity());
        order.setShippingState(request.getShippingState());
        order.setShippingZipCode(request.getShippingZipCode());
        order.setShippingCountry(request.getShippingCountry());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setCustomerEmail(request.getCustomerEmail());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus("PENDING");

        order = orderRepository.save(order);
        orderRepository.flush(); // Ensure timestamps are populated

        for (CartItem cartItem : cart.getItems()) {
            BigDecimal price = cartItem.getProduct().getDiscountPrice() != null ?
                    cartItem.getProduct().getDiscountPrice() : cartItem.getProduct().getPrice();

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(cartItem.getProduct())
                    .quantity(cartItem.getQuantity())
                    .price(price)
                    .productName(cartItem.getProduct().getName())
                    .productImage(cartItem.getProduct().getImages() != null &&
                            !cartItem.getProduct().getImages().isEmpty() ?
                            cartItem.getProduct().getImages().get(0) : null)
                    .build();

            order.getItems().add(orderItem);
            orderItemRepository.save(orderItem);

            Product product = cartItem.getProduct();
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            // Create stock movement ledger entry for this order
            Integer currentStock = stockMovementRepository.getCurrentStock(product.getId());
            if (currentStock == null) {
                currentStock = product.getStockQuantity() + cartItem.getQuantity(); // Old stock before reduction
            }
            Integer newBalance = currentStock - cartItem.getQuantity();

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
            throw new BadRequestException("Order does not belong to user");
        }

        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(Pageable pageable) {
        Page<Order> page = orderRepository.findAll(pageable);
        return mapToPageResponse(page);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByIdAdmin(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        return mapToResponse(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        try {
            OrderStatus status = OrderStatus.valueOf(request.getStatus().toUpperCase());
            order.setStatus(status);

            if (status == OrderStatus.DELIVERED) {
                order.setDeliveredAt(LocalDateTime.now());
                order.setPaymentStatus("COMPLETED");
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid order status");
        }

        if (request.getTrackingNumber() != null) {
            order.setTrackingNumber(request.getTrackingNumber());
        }

        order = orderRepository.save(order);
        return mapToResponse(order);
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
                        .customerName(order.getUser().getFirstName() + " " + order.getUser().getLastName())
                        .totalAmount(order.getTotalAmount())
                        .status(order.getStatus().name())
                        .createdAt(order.getCreatedAt().format(formatter))
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
                .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().format(formatter) : LocalDateTime.now().format(formatter))
                .deliveredAt(order.getDeliveredAt() != null ? order.getDeliveredAt().format(formatter) : null)
                .build();
    }

    private OrderItemResponse mapToItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProductName())
                .productImage(item.getProductImage())
                .quantity(item.getQuantity())
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