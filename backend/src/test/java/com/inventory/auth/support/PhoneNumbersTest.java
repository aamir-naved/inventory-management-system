package com.inventory.auth.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PhoneNumbersTest {

    @Test
    void normalizesTenDigitIndianMobile() {
        assertThat(PhoneNumbers.normalize("98765 43210")).isEqualTo("+919876543210");
        assertThat(PhoneNumbers.normalize("+91-9876543210")).isEqualTo("+919876543210");
        assertThat(PhoneNumbers.normalize("919876543210")).isEqualTo("+919876543210");
    }

    @Test
    void rejectsShortNumbers() {
        assertThatThrownBy(() -> PhoneNumbers.normalize("12345"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
