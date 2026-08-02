package com.inventory.purchase.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseItemResponse(
    UUID productId,
    String productName,
    String unit,
    BigDecimal quantity,
    BigDecimal purchasePrice,
    BigDecimal lineTotal
) {
}
