package com.inventory.tax;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

public final class GstCalculator {

    public static final Set<BigDecimal> ALLOWED_RATES = Set.of(
        new BigDecimal("0.00"),
        new BigDecimal("5.00"),
        new BigDecimal("12.00"),
        new BigDecimal("18.00"),
        new BigDecimal("28.00")
    );

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int MONEY_SCALE = 2;

    private GstCalculator() {
    }

    public static BigDecimal normalizeRate(BigDecimal gstRate) {
        BigDecimal rate = scale(gstRate == null ? BigDecimal.ZERO : gstRate);
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(new BigDecimal("28.00")) > 0) {
            throw new IllegalArgumentException("GST rate must be between 0 and 28");
        }
        BigDecimal matched = ALLOWED_RATES.stream()
            .filter(allowed -> allowed.compareTo(rate) == 0)
            .findFirst()
            .orElse(null);
        if (matched == null) {
            throw new IllegalArgumentException("GST rate must be 0, 5, 12, 18, or 28");
        }
        return matched;
    }

    public static GstLine compute(
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal gstRate,
        boolean inclusive,
        boolean interstate
    ) {
        BigDecimal qty = quantity == null ? BigDecimal.ZERO : quantity;
        BigDecimal price = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        BigDecimal rate = normalizeRate(gstRate);
        BigDecimal gross = qty.multiply(price);
        BigDecimal taxable;
        BigDecimal tax;

        if (rate.compareTo(BigDecimal.ZERO) == 0) {
            taxable = scale(gross);
            tax = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        } else if (inclusive) {
            BigDecimal divisor = BigDecimal.ONE.add(rate.divide(HUNDRED, 6, RoundingMode.HALF_UP));
            taxable = scale(gross.divide(divisor, 6, RoundingMode.HALF_UP));
            tax = scale(gross).subtract(taxable);
        } else {
            taxable = scale(gross);
            tax = scale(taxable.multiply(rate).divide(HUNDRED, 6, RoundingMode.HALF_UP));
        }

        BigDecimal cgst = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal sgst = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal igst = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (tax.compareTo(BigDecimal.ZERO) > 0) {
            if (interstate) {
                igst = tax;
            } else {
                cgst = scale(tax.divide(new BigDecimal("2"), MONEY_SCALE, RoundingMode.HALF_UP));
                sgst = tax.subtract(cgst);
            }
        }

        BigDecimal lineTotal = taxable.add(cgst).add(sgst).add(igst);
        return new GstLine(rate, taxable, cgst, sgst, igst, tax, lineTotal);
    }

    /**
     * Credits a return against a snapshot line total (tax inclusive of the original bill).
     * The final return for a line absorbs any rounding remainder so credits sum to the line total.
     */
    public static BigDecimal proportionalLineCredit(
        BigDecimal originalLineTotal,
        BigDecimal originalQuantity,
        BigDecimal returnQuantity,
        BigDecimal alreadyReturnedQuantity,
        BigDecimal alreadyCreditedAmount
    ) {
        if (originalQuantity == null || originalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Original quantity must be positive");
        }
        if (returnQuantity == null || returnQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Return quantity must be positive");
        }

        BigDecimal lineTotal = scale(originalLineTotal);
        BigDecimal alreadyQty = alreadyReturnedQuantity == null ? BigDecimal.ZERO : alreadyReturnedQuantity;
        BigDecimal alreadyCredited = scale(alreadyCreditedAmount);
        BigDecimal remainingQty = originalQuantity.subtract(alreadyQty);

        if (returnQuantity.compareTo(remainingQty) == 0) {
            return scale(lineTotal.subtract(alreadyCredited));
        }

        return scale(
            lineTotal.multiply(returnQuantity).divide(originalQuantity, 6, RoundingMode.HALF_UP)
        );
    }

    private static BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
