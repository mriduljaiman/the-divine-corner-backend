package com.divinecorner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder @NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private UUID id;
    private String orderNumber;
    private List<OrderItemResponse> items;
    private BigDecimal subtotal;
    private BigDecimal shippingCharges;
    private BigDecimal discount;
    private BigDecimal totalAmount;
    private String status;
    private String shippingAddress;
    private String shippingCity;
    private String shippingState;
    private String shippingZipCode;
    private String shippingCountry;
    private String customerPhone;
    private String customerEmail;
    private String paymentMethod;
    private String paymentStatus;
    private String trackingNumber;
    private String createdAt;
    private String deliveredAt;
}
