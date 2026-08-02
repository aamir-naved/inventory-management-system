package com.inventory.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PurchaseReportResponse(
    OffsetDateTime generatedAt,
    LocalDate from,
    LocalDate to,
    long rowCount,
    BigDecimal totalAmount,
    BigDecimal totalAmountPaid,
    BigDecimal totalOutstanding,
    List<Row> rows
) {
    public record Row(
        UUID purchaseId,
        String purchaseNumber,
        LocalDate purchaseDate,
        UUID supplierId,
        String supplierName,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        BigDecimal outstandingAmount,
        String paymentStatus
    ) {
    }
}
