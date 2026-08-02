package com.inventory.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardMetricsResponse(
    LocalDate asOfDate,
    BigDecimal todaysSalesAmount,
    BigDecimal todaysPurchasesAmount,
    BigDecimal totalRevenue,
    long totalProducts,
    BigDecimal inventoryValue,
    long lowStockProducts,
    BigDecimal outstandingCustomers,
    BigDecimal outstandingSuppliers
) {
}
