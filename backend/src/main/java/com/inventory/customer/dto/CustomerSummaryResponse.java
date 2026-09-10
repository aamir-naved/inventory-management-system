package com.inventory.customer.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CustomerSummaryResponse(
    UUID id,
    UUID businessId,
    String name,
    String contactPerson,
    String mobileNumber,
    String addressLine,
    String stateCode,
    boolean archived,
    long invoiceCount,
    BigDecimal netBilled,
    BigDecimal amountPaid,
    BigDecimal outstandingAmount,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
