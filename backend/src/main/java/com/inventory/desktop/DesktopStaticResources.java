package com.inventory.desktop;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

public final class DesktopStaticResources {

    public static final String STATIC_ROOT = "static";
    public static final String FALLBACK_PAGE = "desktop/missing-ui.html";

    private DesktopStaticResources() {
    }

    public record ResourceFile(byte[] bytes, String contentType, String cacheControl) {
    }

    public static boolean isApiRequest(String requestUri) {
        String path = normalizeUri(requestUri);
        return path.equals("/api") || path.startsWith("/api/");
    }

    public static Optional<ResourceFile> load(String requestUri, ClassLoader classLoader) {
        return load(requestUri, classLoader, STATIC_ROOT);
    }

    static Optional<ResourceFile> load(String requestUri, ClassLoader classLoader, String staticRoot) {
        String path = normalizeUri(requestUri);
        if (isApiRequest(path) || path.contains("..")) {
            return Optional.empty();
        }

        String relative = path.equals("/") ? "index.html" : path.substring(1);
        boolean hashedAsset = relative.startsWith("assets/");
        Optional<byte[]> exact = readClasspath(classLoader, staticRoot + "/" + relative);
        if (exact.isPresent()) {
            return Optional.of(new ResourceFile(
                exact.get(),
                contentType(relative),
                hashedAsset ? "public, max-age=31536000, immutable" : "no-store"
            ));
        }

        if (hashedAsset || hasFileExtension(relative)) {
            return Optional.empty();
        }

        Optional<byte[]> index = readClasspath(classLoader, staticRoot + "/index.html");
        if (index.isPresent()) {
            return Optional.of(new ResourceFile(index.get(), "text/html; charset=UTF-8", "no-store"));
        }

        Optional<byte[]> fallback = readClasspath(classLoader, FALLBACK_PAGE);
        return fallback.map(bytes -> new ResourceFile(bytes, "text/html; charset=UTF-8", "no-store"));
    }

    static String normalizeUri(String requestUri) {
        String raw = requestUri == null || requestUri.isBlank() ? "/" : requestUri;
        int query = raw.indexOf('?');
        if (query >= 0) {
            raw = raw.substring(0, query);
        }
        String decoded = URLDecoder.decode(raw, StandardCharsets.UTF_8);
        if (decoded.isBlank()) {
            return "/";
        }
        return decoded.startsWith("/") ? decoded : "/" + decoded;
    }

    private static boolean hasFileExtension(String relative) {
        int slash = relative.lastIndexOf('/');
        int dot = relative.lastIndexOf('.');
        return dot > slash;
    }

    private static Optional<byte[]> readClasspath(ClassLoader classLoader, String resource) {
        try (InputStream input = classLoader.getResourceAsStream(resource)) {
            if (input == null) {
                return Optional.empty();
            }
            return Optional.of(input.readAllBytes());
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    static String contentType(String relative) {
        String lower = relative.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".html")) {
            return "text/html; charset=UTF-8";
        }
        if (lower.endsWith(".js") || lower.endsWith(".mjs")) {
            return "text/javascript; charset=UTF-8";
        }
        if (lower.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".woff2")) {
            return "font/woff2";
        }
        if (lower.endsWith(".json") || lower.endsWith(".map")) {
            return "application/json";
        }
        if (lower.endsWith(".ico")) {
            return "image/x-icon";
        }
        return "application/octet-stream";
    }
}
