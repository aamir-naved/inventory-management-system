package com.inventory.inventory.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InventoryStockResponse(
    UUID productId,
    String productName,
    String sku,
    String category,
    String unit,
    BigDecimal currentStock,
    BigDecimal costPrice,
    BigDecimal sellingPrice,
    BigDecimal stockValue,
    BigDecimal lowStockThreshold,
    boolean lowStock,
    boolean archived
) {
}
