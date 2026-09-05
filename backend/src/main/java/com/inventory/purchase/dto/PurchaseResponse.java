package com.inventory.purchase.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PurchaseResponse(
    UUID id,
    UUID businessId,
    String purchaseNumber,
    UUID supplierId,
    String supplierName,
    LocalDate purchaseDate,
    String paymentStatus,
    String notes,
    BigDecimal totalAmount,
    BigDecimal returnedAmount,
    BigDecimal netAmount,
    BigDecimal amountPaid,
    BigDecimal outstandingAmount,
    boolean cancelled,
    String cancellationReason,
    boolean hasReturns,
    boolean interstate,
    BigDecimal taxableAmount,
    BigDecimal cgstAmount,
    BigDecimal sgstAmount,
    BigDecimal igstAmount,
    List<PurchaseItemResponse> items,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
