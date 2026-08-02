package com.inventory.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SaleReturnResponse(
    UUID id,
    UUID businessId,
    UUID saleId,
    String saleNumber,
    String customerName,
    String returnNumber,
    LocalDate returnDate,
    String reason,
    String notes,
    BigDecimal totalAmount,
    List<SaleReturnItemResponse> items,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
