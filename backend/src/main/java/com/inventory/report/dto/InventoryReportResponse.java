package com.inventory.report.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record InventoryReportResponse(
    OffsetDateTime generatedAt,
    long totalProducts,
    BigDecimal totalStockValue,
    long lowStockProducts,
    List<Row> rows
) {
    public record Row(
        UUID productId,
        String productName,
        String sku,
        BigDecimal currentStock,
        BigDecimal costPrice,
        BigDecimal sellingPrice,
        BigDecimal stockValue,
        boolean lowStock
    ) {
    }
}
