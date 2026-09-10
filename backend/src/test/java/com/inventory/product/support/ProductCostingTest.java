package com.inventory.product.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ProductCostingTest {

    @Test
    void blendsExistingStockWithInboundPurchase() {
        BigDecimal cost = ProductCosting.weightedAverage(
            new BigDecimal("120.000"),
            new BigDecimal("320.00"),
            new BigDecimal("20.000"),
            new BigDecimal("6300.00")
        );
        assertThat(cost).isEqualByComparingTo("319.29");
    }

    @Test
    void usesInboundCostWhenShelfWasEmpty() {
        BigDecimal cost = ProductCosting.weightedAverage(
            BigDecimal.ZERO,
            new BigDecimal("320.00"),
            new BigDecimal("10.000"),
            new BigDecimal("1500.00")
        );
        assertThat(cost).isEqualByComparingTo("150.00");
    }
}
