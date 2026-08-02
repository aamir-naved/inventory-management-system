package com.inventory.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SalesReportResponse(
    OffsetDateTime generatedAt,
    LocalDate from,
    LocalDate to,
    long rowCount,
    BigDecimal totalNetAmount,
    BigDecimal totalAmountPaid,
    BigDecimal totalOutstanding,
    List<Row> rows
) {
    public record Row(
        UUID saleId,
        String saleNumber,
        LocalDate saleDate,
        UUID customerId,
        String customerName,
        BigDecimal netAmount,
        BigDecimal amountPaid,
        BigDecimal outstandingAmount,
        String paymentStatus
    ) {
    }
}
