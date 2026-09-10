package com.inventory.tax;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GstPlaceOfSupplyTest {

    @Test
    void sameStateIsIntrastate() {
        assertThat(GstPlaceOfSupply.isInterstate("29", "29")).isFalse();
        assertThat(GstPlaceOfSupply.isInterstate("29", " 29 ")).isFalse();
    }

    @Test
    void differentStateIsInterstate() {
        assertThat(GstPlaceOfSupply.isInterstate("29", "27")).isTrue();
    }

    @Test
    void missingPartyStateDefaultsIntrastate() {
        assertThat(GstPlaceOfSupply.isInterstate("29", null)).isFalse();
        assertThat(GstPlaceOfSupply.isInterstate("29", "")).isFalse();
        assertThat(GstPlaceOfSupply.isInterstate("29", "  ")).isFalse();
    }

    @Test
    void missingBusinessStateDefaultsIntrastate() {
        assertThat(GstPlaceOfSupply.isInterstate(null, "27")).isFalse();
        assertThat(GstPlaceOfSupply.isInterstate("", "27")).isFalse();
    }
}
