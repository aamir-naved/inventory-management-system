package com.inventory.desktop;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DesktopStaticResourcesTest {

    @TempDir
    Path tempDir;

    @Test
    void treatsApiPathsAsPassThrough() {
        assertThat(DesktopStaticResources.isApiRequest("/api/actuator/health")).isTrue();
        assertThat(DesktopStaticResources.isApiRequest("/api")).isTrue();
        assertThat(DesktopStaticResources.isApiRequest("/pos")).isFalse();
        assertThat(DesktopStaticResources.isApiRequest("/")).isFalse();
    }

    @Test
    void servesIndexForSpaRoutesAndAssetsFromClasspath() throws Exception {
        Path staticRoot = tempDir.resolve("static");
        Files.createDirectories(staticRoot.resolve("assets"));
        Files.writeString(staticRoot.resolve("index.html"), "<html>shop</html>");
        Files.writeString(staticRoot.resolve("assets").resolve("app.js"), "console.log(1)");

        try (URLClassLoader loader = classLoaderFor(tempDir)) {
            var index = DesktopStaticResources.load("/pos", loader, "static").orElseThrow();
            assertThat(new String(index.bytes())).contains("shop");
            assertThat(index.contentType()).contains("text/html");
            assertThat(index.cacheControl()).isEqualTo("no-store");

            var asset = DesktopStaticResources.load("/assets/app.js", loader, "static").orElseThrow();
            assertThat(new String(asset.bytes())).contains("console.log");
            assertThat(asset.contentType()).contains("javascript");
            assertThat(asset.cacheControl()).contains("immutable");
        }
    }

    @Test
    void rejectsMissingHashedAssetsInsteadOfSpaFallback() throws Exception {
        Path staticRoot = tempDir.resolve("static");
        Files.createDirectories(staticRoot);
        Files.writeString(staticRoot.resolve("index.html"), "<html>shop</html>");

        try (URLClassLoader loader = classLoaderFor(tempDir)) {
            assertThat(DesktopStaticResources.load("/assets/missing.js", loader, "static")).isEmpty();
            assertThat(DesktopStaticResources.load("/../application.yml", loader, "static")).isEmpty();
        }
    }

    private static URLClassLoader classLoaderFor(Path root) throws IOException {
        return new URLClassLoader(new URL[] {root.toUri().toURL()}, null);
    }
}
