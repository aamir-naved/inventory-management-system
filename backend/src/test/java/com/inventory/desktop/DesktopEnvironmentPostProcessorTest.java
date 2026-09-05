package com.inventory.desktop;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class DesktopEnvironmentPostProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesAndReusesJwtSecretForDesktopProfile() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("desktop");
        environment.setProperty("APP_DATA_DIR", tempDir.toString());

        DesktopEnvironmentPostProcessor processor = new DesktopEnvironmentPostProcessor();
        processor.postProcessEnvironment(environment, new SpringApplication());

        String firstSecret = environment.getProperty("app.auth.jwt-secret");
        assertThat(firstSecret).isNotBlank().hasSizeGreaterThan(31);
        assertThat(environment.getProperty("app.desktop.data-dir")).isEqualTo(tempDir.toString());
        assertThat(environment.getProperty("app.desktop.enabled")).isEqualTo("true");

        Path stored = tempDir.resolve(DesktopPaths.PROPERTIES_FILE);
        assertThat(stored).exists();
        Properties properties = new Properties();
        try (var input = Files.newInputStream(stored)) {
            properties.load(input);
        }
        assertThat(properties.getProperty(DesktopPaths.JWT_SECRET_KEY)).isEqualTo(firstSecret);

        MockEnvironment second = new MockEnvironment();
        second.setActiveProfiles("desktop");
        second.setProperty("APP_DATA_DIR", tempDir.toString());
        processor.postProcessEnvironment(second, new SpringApplication());
        assertThat(second.getProperty("app.auth.jwt-secret")).isEqualTo(firstSecret);
    }

    @Test
    void ignoresNonDesktopProfiles() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        environment.setProperty("APP_DATA_DIR", tempDir.toString());

        new DesktopEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("app.auth.jwt-secret")).isNull();
        assertThat(Files.exists(tempDir.resolve(DesktopPaths.PROPERTIES_FILE))).isFalse();
    }
}
