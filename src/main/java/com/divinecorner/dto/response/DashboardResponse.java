package com.divinecorner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private Long totalOrders;
    private Long pendingOrders;
    private Long deliveredOrders;
    private BigDecimal totalRevenue;
    private BigDecimal monthlyRevenue;
    private Long totalProducts;
    private Long lowStockProducts;
    private Long totalUsers;
    private List<RecentOrderResponse> recentOrders;
}
