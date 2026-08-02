package com.inventory.sales.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SaleReturnItemResponse(
    UUID id,
    UUID saleItemId,
    UUID productId,
    String productName,
    String unit,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal lineTotal
) {
}
