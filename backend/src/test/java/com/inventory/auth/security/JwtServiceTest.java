package com.inventory.auth.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.inventory.config.AuthProperties;

class JwtServiceTest {

    @Test
    void refusesBlankSecret() {
        AuthProperties properties = new AuthProperties();
        properties.setJwtSecret("  ");

        assertThatThrownBy(() -> new JwtService(properties))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("APP_JWT_SECRET");
    }

    @Test
    void refusesMissingSecret() {
        assertThatThrownBy(() -> new JwtService(new AuthProperties()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("APP_JWT_SECRET");
    }
}
