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
                "spring.datasource.password=a-strong-db-password",
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
                "spring.datasource.password=a-strong-db-password",
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
                "spring.datasource.password=a-strong-db-password",
                "app.auth.public-app-url=https://shop.example.com"
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).hasStackTraceContaining("SPRING_MAIL_HOST");
            });
    }

    @Test
    void prodWithoutSmsWebhookFails() {
        prodRunner
            .withPropertyValues(
                "app.auth.jwt-secret=" + STRONG_JWT_SECRET,
                "spring.datasource.password=a-strong-db-password",
                "spring.mail.host=localhost",
                "app.auth.public-app-url=https://shop.example.com",
                "app.platform.open-registration=true"
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).hasStackTraceContaining("APP_SMS_WEBHOOK_URL");
            });
    }

    @Test
    void prodWithKnownDevDbPasswordFails() {
        prodRunner
            .withPropertyValues(
                "app.auth.jwt-secret=" + STRONG_JWT_SECRET,
                "spring.datasource.password=" + ProductionSafety.KNOWN_DEV_DB_PASSWORD,
                "spring.mail.host=localhost",
                "app.auth.public-app-url=https://shop.example.com",
                "app.auth.sms-webhook-url=https://sms.example.com/send",
                "app.platform.open-registration=true"
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).hasStackTraceContaining("inventory_password");
            });
    }

    @Test
    void prodWithRequiredSettingsStarts() {
        prodRunner
            .withPropertyValues(
                "app.auth.jwt-secret=" + STRONG_JWT_SECRET,
                "spring.datasource.password=a-strong-db-password",
                "spring.mail.host=localhost",
                "app.auth.public-app-url=https://shop.example.com",
                "app.auth.sms-webhook-url=https://sms.example.com/send",
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
