package com.inventory.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SaleResponse(
    UUID id,
    UUID businessId,
    String saleNumber,
    UUID customerId,
    String customerName,
    LocalDate saleDate,
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
    List<SaleItemResponse> items,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
