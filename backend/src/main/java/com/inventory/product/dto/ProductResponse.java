package com.inventory.product.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    UUID businessId,
    String name,
    String sku,
    String category,
    String unit,
    BigDecimal costPrice,
    BigDecimal sellingPrice,
    BigDecimal openingStock,
    BigDecimal currentStock,
    BigDecimal lowStockThreshold,
    boolean archived,
    String barcode,
    String hsnCode,
    BigDecimal gstRate,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
