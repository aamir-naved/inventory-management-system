package com.inventory.tax;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GstCalculatorTest {

    @Test
    void exclusiveIntraStateSplitsCgstAndSgst() {
        GstLine line = GstCalculator.compute(
            new BigDecimal("2"),
            new BigDecimal("100.00"),
            new BigDecimal("18"),
            false,
            false
        );

        assertThat(line.taxableAmount()).isEqualByComparingTo("200.00");
        assertThat(line.cgstAmount()).isEqualByComparingTo("18.00");
        assertThat(line.sgstAmount()).isEqualByComparingTo("18.00");
        assertThat(line.igstAmount()).isEqualByComparingTo("0.00");
        assertThat(line.lineTotal()).isEqualByComparingTo("236.00");
    }

    @Test
    void exclusiveInterstateUsesIgst() {
        GstLine line = GstCalculator.compute(
            BigDecimal.ONE,
            new BigDecimal("100.00"),
            new BigDecimal("18"),
            false,
            true
        );

        assertThat(line.igstAmount()).isEqualByComparingTo("18.00");
        assertThat(line.cgstAmount()).isEqualByComparingTo("0.00");
        assertThat(line.lineTotal()).isEqualByComparingTo("118.00");
    }

    @Test
    void inclusivePriceExtractsTax() {
        GstLine line = GstCalculator.compute(
            BigDecimal.ONE,
            new BigDecimal("118.00"),
            new BigDecimal("18"),
            true,
            false
        );

        assertThat(line.taxableAmount()).isEqualByComparingTo("100.00");
        assertThat(line.lineTotal()).isEqualByComparingTo("118.00");
    }

    @Test
    void rejectsUnknownRate() {
        assertThatThrownBy(() -> GstCalculator.normalizeRate(new BigDecimal("10")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void proportionalLineCreditIncludesTaxAndAbsorbsRemainderOnFinalReturn() {
        BigDecimal lineTotal = new BigDecimal("118.00");
        BigDecimal quantity = new BigDecimal("3.000");

        BigDecimal first = GstCalculator.proportionalLineCredit(
            lineTotal,
            quantity,
            new BigDecimal("1.000"),
            BigDecimal.ZERO,
            BigDecimal.ZERO
        );
        BigDecimal second = GstCalculator.proportionalLineCredit(
            lineTotal,
            quantity,
            new BigDecimal("1.000"),
            new BigDecimal("1.000"),
            first
        );
        BigDecimal finalCredit = GstCalculator.proportionalLineCredit(
            lineTotal,
            quantity,
            new BigDecimal("1.000"),
            new BigDecimal("2.000"),
            first.add(second)
        );

        assertThat(first).isEqualByComparingTo("39.33");
        assertThat(second).isEqualByComparingTo("39.33");
        assertThat(finalCredit).isEqualByComparingTo("39.34");
        assertThat(first.add(second).add(finalCredit)).isEqualByComparingTo(lineTotal);
    }
}
