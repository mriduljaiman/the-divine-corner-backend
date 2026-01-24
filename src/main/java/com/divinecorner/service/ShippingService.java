package com.divinecorner.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class ShippingService {

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500");
    private static final BigDecimal BASE_SHIPPING_CHARGE = new BigDecimal("50");
    private static final BigDecimal PER_KG_CHARGE = new BigDecimal("20");

    /**
     * Calculate shipping charges based on order value and weight
     *
     * Rules:
     * - Free shipping for orders above ₹500
     * - Base charge ₹50 + ₹20 per kg
     */
    public BigDecimal calculateShippingCharges(BigDecimal subtotal, Double weightInKg) {
        // Free shipping for orders above threshold
        if (subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            return BigDecimal.ZERO;
        }

        // Calculate based on weight (if provided)
        if (weightInKg != null && weightInKg > 0) {
            BigDecimal weightCharge = PER_KG_CHARGE.multiply(BigDecimal.valueOf(weightInKg));
            return BASE_SHIPPING_CHARGE.add(weightCharge);
        }

        // Default base charge
        return BASE_SHIPPING_CHARGE;
    }

    /**
     * Calculate shipping based on pin code zones (you can expand this)
     */
    public BigDecimal calculateShippingByPincode(BigDecimal subtotal, String pincode) {
        if (subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            return BigDecimal.ZERO;
        }

        // You can add logic for different zones
        // For now, return base charge
        return BASE_SHIPPING_CHARGE;
    }
}
