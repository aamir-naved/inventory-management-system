package com.inventory.supplier.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SupplierSummaryResponse(
    UUID id,
    UUID businessId,
    String name,
    String contactPerson,
    String mobileNumber,
    String addressLine,
    boolean archived,
    long billCount,
    BigDecimal billedAmount,
    BigDecimal amountPaid,
    BigDecimal outstandingAmount,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
