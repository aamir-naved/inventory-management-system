package com.inventory.sales.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SaleItemResponse(
    UUID id,
    UUID productId,
    String productName,
    String unit,
    BigDecimal quantity,
    BigDecimal sellingPrice,
    BigDecimal lineTotal,
    String hsnCode,
    BigDecimal gstRate,
    BigDecimal taxableAmount,
    BigDecimal cgstAmount,
    BigDecimal sgstAmount,
    BigDecimal igstAmount,
    BigDecimal returnedQuantity,
    BigDecimal returnableQuantity
) {
}
