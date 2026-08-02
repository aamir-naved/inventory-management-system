package com.inventory.payment.support;

import java.math.BigDecimal;

public final class PaymentAmounts {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PARTIAL = "PARTIAL";
    public static final String STATUS_PAID = "PAID";

    private PaymentAmounts() {
    }

    public static BigDecimal outstanding(BigDecimal amountPaid, BigDecimal billableAmount) {
        BigDecimal paid = nullSafe(amountPaid);
        BigDecimal billable = nullSafe(billableAmount);
        BigDecimal due = billable.subtract(paid);
        return due.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : due;
    }

    public static String deriveStatus(BigDecimal amountPaid, BigDecimal billableAmount) {
        BigDecimal paid = nullSafe(amountPaid);
        BigDecimal outstanding = outstanding(paid, billableAmount);
        if (paid.compareTo(BigDecimal.ZERO) == 0) {
            return STATUS_PENDING;
        }
        if (outstanding.compareTo(BigDecimal.ZERO) == 0) {
            return STATUS_PAID;
        }
        return STATUS_PARTIAL;
    }

    private static BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
