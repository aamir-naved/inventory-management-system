package com.inventory.purchase.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PurchaseReturnResponse(
    UUID id,
    UUID businessId,
    UUID purchaseId,
    String purchaseNumber,
    String supplierName,
    String returnNumber,
    LocalDate returnDate,
    String reason,
    String notes,
    BigDecimal totalAmount,
    List<PurchaseReturnItemResponse> items,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
