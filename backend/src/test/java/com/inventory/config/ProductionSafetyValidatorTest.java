package com.inventory.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ProductionSafetyValidatorTest {

    private static final String STRONG_JWT_SECRET = "a-sufficiently-long-production-jwt-secret-value";

    private final ApplicationContextRunner prodRunner = new ApplicationContextRunner()
        .withUserConfiguration(ProductionSafetyValidator.class)
        .withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"));

    @Test
    void prodWithoutJwtSecretFails() {
        prodRunner
            .withPropertyValues(
                "spring.mail.host=localhost",
                "app.auth.public-app-url=https://shop.example.com"
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).hasStackTraceContaining("APP_JWT_SECRET");
            });
    }

    @Test
    void prodWithKnownDevJwtSecretFails() {
        prodRunner
            .withPropertyValues(
                "app.auth.jwt-secret=" + ProductionSafety.KNOWN_DEV_JWT_SECRET,
                "spring.mail.host=localhost",
                "app.auth.public-app-url=https://shop.example.com"
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).hasStackTraceContaining("development default");
            });
    }

    @Test
    void prodWithoutMailHostFails() {
        prodRunner
            .withPropertyValues(
                "app.auth.jwt-secret=" + STRONG_JWT_SECRET,
                "app.auth.public-app-url=https://shop.example.com"
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).hasStackTraceContaining("SPRING_MAIL_HOST");
            });
    }

    @Test
    void prodWithRequiredSettingsStarts() {
        prodRunner
            .withPropertyValues(
                "app.auth.jwt-secret=" + STRONG_JWT_SECRET,
                "spring.mail.host=localhost",
                "app.auth.public-app-url=https://shop.example.com",
                "app.platform.open-registration=false",
                "app.platform.admin-email=admin@example.com",
                "app.platform.admin-password=a-long-enough-admin-password"
            )
            .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void validatorIsInactiveOutsideProd() {
        new ApplicationContextRunner()
            .withUserConfiguration(ProductionSafetyValidator.class)
            .withInitializer(context -> context.getEnvironment().setActiveProfiles("dev"))
            .run(context -> assertThat(context).hasNotFailed());
    }
}
