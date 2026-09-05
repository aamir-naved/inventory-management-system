package com.inventory.config;

import org.springframework.core.env.Environment;

public final class ProductionSafety {

    public static final String KNOWN_DEV_JWT_SECRET =
        "inventory-management-system-super-secret-key-for-development-only-2026";
    public static final int MIN_JWT_SECRET_LENGTH = 32;
    public static final int MIN_PLATFORM_ADMIN_PASSWORD_LENGTH = 12;

    private ProductionSafety() {
    }

    public static void validate(Environment environment) {
        String jwtSecret = firstNonBlank(
            environment.getProperty("app.auth.jwt-secret"),
            environment.getProperty("APP_JWT_SECRET")
        );
        if (jwtSecret == null) {
            throw new IllegalStateException(
                "APP_JWT_SECRET must be set when using the prod profile."
            );
        }
        if (jwtSecret.length() < MIN_JWT_SECRET_LENGTH) {
            throw new IllegalStateException(
                "APP_JWT_SECRET must be at least " + MIN_JWT_SECRET_LENGTH
                    + " characters when using the prod profile."
            );
        }
        if (KNOWN_DEV_JWT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException(
                "APP_JWT_SECRET must not use the development default when using the prod profile."
            );
        }

        String mailHost = firstNonBlank(
            environment.getProperty("spring.mail.host"),
            environment.getProperty("SPRING_MAIL_HOST")
        );
        if (mailHost == null) {
            throw new IllegalStateException(
                "SPRING_MAIL_HOST must be set when using the prod profile so verification and password-reset emails are delivered."
            );
        }

        String publicAppUrl = firstNonBlank(
            environment.getProperty("app.auth.public-app-url"),
            environment.getProperty("APP_PUBLIC_APP_URL")
        );
        if (publicAppUrl == null) {
            throw new IllegalStateException(
                "APP_PUBLIC_APP_URL must be set when using the prod profile so verify/reset email links point at the public UI."
            );
        }

        if (!openRegistration(environment)) {
            String adminEmail = firstNonBlank(
                environment.getProperty("app.platform.admin-email"),
                environment.getProperty("APP_PLATFORM_ADMIN_EMAIL")
            );
            if (adminEmail == null) {
                throw new IllegalStateException(
                    "APP_PLATFORM_ADMIN_EMAIL must be set when using the prod profile with closed registration."
                );
            }
            String adminPassword = firstNonBlank(
                environment.getProperty("app.platform.admin-password"),
                environment.getProperty("APP_PLATFORM_ADMIN_PASSWORD")
            );
            if (adminPassword == null) {
                throw new IllegalStateException(
                    "APP_PLATFORM_ADMIN_PASSWORD must be set when using the prod profile with closed registration."
                );
            }
            if (adminPassword.length() < MIN_PLATFORM_ADMIN_PASSWORD_LENGTH) {
                throw new IllegalStateException(
                    "APP_PLATFORM_ADMIN_PASSWORD must be at least "
                        + MIN_PLATFORM_ADMIN_PASSWORD_LENGTH
                        + " characters when using the prod profile."
                );
            }
        }
    }

    private static boolean openRegistration(Environment environment) {
        String raw = firstNonBlank(
            environment.getProperty("app.platform.open-registration"),
            environment.getProperty("APP_OPEN_REGISTRATION")
        );
        return "true".equalsIgnoreCase(raw);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (isUsable(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static boolean isUsable(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String trimmed = value.trim();
        return !(trimmed.startsWith("${") && trimmed.endsWith("}"));
    }
}
