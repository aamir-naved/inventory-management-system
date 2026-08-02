package com.inventory.report.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SupplierOutstandingReportResponse(
    OffsetDateTime generatedAt,
    long supplierCount,
    BigDecimal totalOutstanding,
    List<Row> rows
) {
    public record Row(
        UUID supplierId,
        String supplierName,
        long billCount,
        BigDecimal billedAmount,
        BigDecimal amountPaid,
        BigDecimal outstandingAmount
    ) {
    }
}
