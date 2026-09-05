package com.inventory.desktop;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class DesktopPaths {

    public static final String PROPERTIES_FILE = "desktop.properties";
    public static final String PENDING_RESTORE_FILE = "restore-pending.sql.gz";
    public static final String JWT_SECRET_KEY = "jwt.secret";

    private DesktopPaths() {
    }

    public static Path defaultDataDir() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (usable(localAppData)) {
            return Path.of(localAppData.trim(), "InventoryManagement");
        }
        String xdg = System.getenv("XDG_DATA_HOME");
        if (usable(xdg)) {
            return Path.of(xdg.trim(), "inventory-management");
        }
        String home = System.getProperty("user.home", ".");
        Path macSupport = Path.of(home, "Library", "Application Support");
        if (Files.isDirectory(macSupport)) {
            return macSupport.resolve("InventoryManagement");
        }
        return Path.of(home, ".local", "share", "inventory-management");
    }

    public static Path resolveDataDir(String configured) {
        if (usable(configured)) {
            return Path.of(configured.trim()).toAbsolutePath().normalize();
        }
        String fromEnv = System.getenv("APP_DATA_DIR");
        if (usable(fromEnv)) {
            return Path.of(fromEnv.trim()).toAbsolutePath().normalize();
        }
        return defaultDataDir().toAbsolutePath().normalize();
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    public static String executableName(String tool) {
        return isWindows() ? tool + ".exe" : tool;
    }

    private static boolean usable(String value) {
        return value != null && !value.isBlank();
    }
}
