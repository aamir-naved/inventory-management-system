package com.inventory.desktop;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class DesktopEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String PROPERTY_SOURCE_NAME = "desktopRuntime";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!isDesktopProfile(environment)) {
            return;
        }

        Path dataDir = DesktopPaths.resolveDataDir(firstNonBlank(
            environment.getProperty("APP_DATA_DIR"),
            environment.getProperty("app.desktop.data-dir")
        ));
        try {
            Files.createDirectories(dataDir);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create desktop data directory: " + dataDir, exception);
        }

        Path propertiesFile = dataDir.resolve(DesktopPaths.PROPERTIES_FILE);
        Properties stored = loadProperties(propertiesFile);

        String jwtSecret = firstNonBlank(
            environment.getProperty("APP_JWT_SECRET"),
            environment.getProperty("app.auth.jwt-secret"),
            stored.getProperty(DesktopPaths.JWT_SECRET_KEY)
        );
        if (!usable(jwtSecret)) {
            jwtSecret = generateSecret();
            stored.setProperty(DesktopPaths.JWT_SECRET_KEY, jwtSecret);
            storeProperties(propertiesFile, stored);
        } else if (!jwtSecret.equals(stored.getProperty(DesktopPaths.JWT_SECRET_KEY))) {
            stored.setProperty(DesktopPaths.JWT_SECRET_KEY, jwtSecret);
            storeProperties(propertiesFile, stored);
        }

        Map<String, Object> runtime = new HashMap<>();
        runtime.put("app.desktop.enabled", true);
        runtime.put("app.desktop.data-dir", dataDir.toString());
        runtime.put("app.auth.jwt-secret", jwtSecret);
        runtime.put("app.auth.public-app-url", firstNonBlank(
            environment.getProperty("APP_PUBLIC_APP_URL"),
            environment.getProperty("app.auth.public-app-url"),
            "http://127.0.0.1:18080"
        ));
        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, runtime));
    }

    static boolean isDesktopProfile(ConfigurableEnvironment environment) {
        if (Arrays.asList(environment.getActiveProfiles()).contains("desktop")) {
            return true;
        }
        String property = environment.getProperty("spring.profiles.active", "");
        return Arrays.stream(property.split(",")).map(String::trim).anyMatch("desktop"::equals);
    }

    private static Properties loadProperties(Path file) {
        Properties properties = new Properties();
        if (!Files.exists(file)) {
            return properties;
        }
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read " + file, exception);
        }
        return properties;
    }

    private static void storeProperties(Path file, Properties properties) {
        try (OutputStream output = Files.newOutputStream(file)) {
            properties.store(output, "Inventory Management desktop runtime");
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write " + file, exception);
        }
    }

    private static String generateSecret() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (usable(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static boolean usable(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String trimmed = value.trim();
        return !(trimmed.startsWith("${") && trimmed.endsWith("}"));
    }
}
