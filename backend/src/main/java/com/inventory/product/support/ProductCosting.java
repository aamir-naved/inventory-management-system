package com.inventory.product.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Weighted-average cost after inbound stock. */
public final class ProductCosting {

    private ProductCosting() {
    }

    /**
     * @param quantityBefore stock on hand before the inbound receipt (may be zero or negative)
     * @param costBefore     catalog unit cost before the receipt
     * @param quantityIn     inbound quantity (&gt; 0)
     * @param valueIn        inbound extended cost (qty × unit cost), money scale
     */
    public static BigDecimal weightedAverage(
        BigDecimal quantityBefore,
        BigDecimal costBefore,
        BigDecimal quantityIn,
        BigDecimal valueIn
    ) {
        BigDecimal qtyIn = nullSafe(quantityIn);
        BigDecimal valIn = nullSafe(valueIn);
        if (qtyIn.compareTo(BigDecimal.ZERO) <= 0) {
            return scaleMoney(nullSafe(costBefore));
        }

        BigDecimal beforeQty = nullSafe(quantityBefore);
        BigDecimal afterQty = beforeQty.add(qtyIn);
        if (afterQty.compareTo(BigDecimal.ZERO) <= 0) {
            return scaleMoney(valIn.divide(qtyIn, 6, RoundingMode.HALF_UP));
        }

        if (beforeQty.compareTo(BigDecimal.ZERO) <= 0) {
            return scaleMoney(valIn.divide(qtyIn, 6, RoundingMode.HALF_UP));
        }

        BigDecimal beforeValue = beforeQty.multiply(nullSafe(costBefore));
        return scaleMoney(beforeValue.add(valIn).divide(afterQty, 6, RoundingMode.HALF_UP));
    }

    private static BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
