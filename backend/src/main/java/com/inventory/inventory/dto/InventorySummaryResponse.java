package com.inventory.inventory.dto;

import java.math.BigDecimal;

public record InventorySummaryResponse(
    long totalProducts,
    long lowStockProducts,
    BigDecimal totalStockValue,
    BigDecimal totalPotentialRevenue
) {
}
