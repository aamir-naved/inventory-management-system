package com.inventory.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionSafetyTest {

    private static final String STRONG_JWT_SECRET = "a-sufficiently-long-production-jwt-secret-value";

    @Test
    void refusesMissingJwtSecret() {
        assertThatThrownBy(() -> ProductionSafety.validate(validProdEnvironment().withProperty("app.auth.jwt-secret", "")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("APP_JWT_SECRET");
    }

    @Test
    void refusesShortJwtSecret() {
        assertThatThrownBy(() -> ProductionSafety.validate(validProdEnvironment().withProperty("app.auth.jwt-secret", "too-short-to-be-an-hmac-key")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("at least 32");
    }

    @Test
    void refusesKnownDevelopmentJwtSecret() {
        assertThatThrownBy(() -> ProductionSafety.validate(
            validProdEnvironment().withProperty("app.auth.jwt-secret", ProductionSafety.KNOWN_DEV_JWT_SECRET)
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("development default");
    }

    @Test
    void refusesMissingMailHost() {
        assertThatThrownBy(() -> ProductionSafety.validate(validProdEnvironment().withProperty("spring.mail.host", "")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SPRING_MAIL_HOST");
    }

    @Test
    void refusesMissingPublicAppUrl() {
        assertThatThrownBy(() -> ProductionSafety.validate(validProdEnvironment().withProperty("app.auth.public-app-url", "")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("APP_PUBLIC_APP_URL");
    }

    @Test
    void treatsUnresolvedPlaceholdersAsMissing() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("app.auth.jwt-secret", "${APP_JWT_SECRET:}")
            .withProperty("spring.mail.host", "localhost")
            .withProperty("app.auth.public-app-url", "https://shop.example.com")
            .withProperty("app.platform.open-registration", "true");

        assertThatThrownBy(() -> ProductionSafety.validate(environment))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("APP_JWT_SECRET");
    }

    @Test
    void acceptsClosedRegistrationWithAdminCredentials() {
        assertThatCode(() -> ProductionSafety.validate(validProdEnvironment()))
            .doesNotThrowAnyException();
    }

    @Test
    void acceptsOpenRegistrationWithoutPlatformAdmin() {
        MockEnvironment environment = validProdEnvironment()
            .withProperty("app.platform.open-registration", "true")
            .withProperty("app.platform.admin-email", "")
            .withProperty("app.platform.admin-password", "");

        assertThatCode(() -> ProductionSafety.validate(environment)).doesNotThrowAnyException();
    }

    @Test
    void refusesClosedRegistrationWithoutAdminEmail() {
        assertThatThrownBy(() -> ProductionSafety.validate(
            validProdEnvironment().withProperty("app.platform.admin-email", "")
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("APP_PLATFORM_ADMIN_EMAIL");
    }

    @Test
    void refusesClosedRegistrationWithShortAdminPassword() {
        assertThatThrownBy(() -> ProductionSafety.validate(
            validProdEnvironment().withProperty("app.platform.admin-password", "short")
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("APP_PLATFORM_ADMIN_PASSWORD");
    }

    private MockEnvironment validProdEnvironment() {
        return new MockEnvironment()
            .withProperty("app.auth.jwt-secret", STRONG_JWT_SECRET)
            .withProperty("spring.mail.host", "localhost")
            .withProperty("app.auth.public-app-url", "https://shop.example.com")
            .withProperty("app.platform.open-registration", "false")
            .withProperty("app.platform.admin-email", "admin@example.com")
            .withProperty("app.platform.admin-password", "a-long-enough-admin-password");
    }
}
