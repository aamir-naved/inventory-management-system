package com.inventory.purchase.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseItemResponse(
    UUID id,
    UUID productId,
    String productName,
    String unit,
    BigDecimal quantity,
    BigDecimal purchasePrice,
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
