package com.inventory.purchase.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseReturnItemResponse(
    UUID id,
    UUID purchaseItemId,
    UUID productId,
    String productName,
    String unit,
    BigDecimal quantity,
    BigDecimal unitCost,
    BigDecimal lineTotal
) {
}
