package com.inventory.tax;

import java.math.BigDecimal;

public record GstLine(
    BigDecimal gstRate,
    BigDecimal taxableAmount,
    BigDecimal cgstAmount,
    BigDecimal sgstAmount,
    BigDecimal igstAmount,
    BigDecimal taxAmount,
    BigDecimal lineTotal
) {
}
