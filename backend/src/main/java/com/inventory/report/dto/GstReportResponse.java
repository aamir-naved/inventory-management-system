package com.inventory.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record GstReportResponse(
    OffsetDateTime generatedAt,
    LocalDate from,
    LocalDate to,
    BigDecimal taxableAmount,
    BigDecimal cgstAmount,
    BigDecimal sgstAmount,
    BigDecimal igstAmount,
    BigDecimal totalTax,
    List<Row> rows
) {
    public record Row(
        String documentType,
        String documentNumber,
        LocalDate documentDate,
        String partyName,
        boolean interstate,
        BigDecimal gstRate,
        BigDecimal taxableAmount,
        BigDecimal cgstAmount,
        BigDecimal sgstAmount,
        BigDecimal igstAmount
    ) {
    }
}
