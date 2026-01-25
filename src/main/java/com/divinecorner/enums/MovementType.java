package com.divinecorner.enums;

public enum MovementType {
    STOCK_IN,      // Stock received
    STOCK_OUT,     // Stock removed
    ONLINE_ORDER,  // Stock out via website order
    OFFLINE_SALE,  // Stock out via offline billing
    ADJUSTMENT,    // Manual correction
    RETURN,        // Return to supplier
    DAMAGED,       // Damaged goods
    EXPIRED,       // Expired products
    SAMPLE,        // Given as sample
    GIFT,          // Given as gift
    OPENING_STOCK  // Initial stock entry
}
