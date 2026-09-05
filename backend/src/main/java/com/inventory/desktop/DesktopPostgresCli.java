package com.inventory.desktop;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.springframework.core.env.Environment;

public final class DesktopPostgresCli {

    private static final Duration COMMAND_TIMEOUT = Duration.ofMinutes(10);

    private DesktopPostgresCli() {
    }

    public static void writeGzipDump(Environment environment, OutputStream output) {
        Path pgDump = requireTool(environment, "pg_dump");
        List<String> command = new ArrayList<>();
        command.add(pgDump.toString());
        command.addAll(connectionArgs(environment));
        command.add("--clean");
        command.add("--if-exists");
        command.add("--no-owner");
        command.add("--no-acl");
        command.add("-F");
        command.add("p");
        command.add("-d");
        command.add(databaseName(environment));

        Process process = start(environment, command);
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            process.getInputStream().transferTo(gzip);
            if (waitFor(process) != 0) {
                throw new DesktopBackupException("Database export failed. Check desktop logs and try again.");
            }
        } catch (IOException exception) {
            process.destroyForcibly();
            throw new DesktopBackupException("Unable to write the backup file.", exception);
        }
    }

    public static void restoreGzipSql(Environment environment, Path gzipSql) {
        Path psql = requireTool(environment, "psql");
        List<String> command = new ArrayList<>();
        command.add(psql.toString());
        command.addAll(connectionArgs(environment));
        command.add("-d");
        command.add(databaseName(environment));
        command.add("-v");
        command.add("ON_ERROR_STOP=1");

        Process process = start(environment, command);
        try (InputStream gzip = new GZIPInputStream(Files.newInputStream(gzipSql));
             OutputStream stdin = process.getOutputStream()) {
            gzip.transferTo(stdin);
        } catch (IOException exception) {
            process.destroyForcibly();
            throw new DesktopBackupException("Unable to read the backup file.", exception);
        }
        if (waitFor(process) != 0) {
            throw new DesktopBackupException("Database restore failed.");
        }
    }

    static Path requireTool(Environment environment, String name) {
        String configured = firstNonBlank(
            environment.getProperty("APP_DESKTOP_PG_BIN"),
            environment.getProperty("app.desktop.pg-bin")
        );
        if (configured != null) {
            Path tool = Path.of(configured).resolve(DesktopPaths.executableName(name));
            if (Files.exists(tool)) {
                return tool.toAbsolutePath().normalize();
            }
        }
        throw new DesktopBackupException(
            "PostgreSQL tools were not found. Reinstall the desktop app or set APP_DESKTOP_PG_BIN."
        );
    }

    private static List<String> connectionArgs(Environment environment) {
        return List.of(
            "-h", host(environment),
            "-p", Integer.toString(port(environment)),
            "-U", username(environment)
        );
    }

    private static Process start(Environment environment, List<String> command) {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Map<String, String> env = builder.environment();
        env.put("PGPASSWORD", password(environment));
        env.put("PGCLIENTENCODING", "UTF8");
        try {
            return builder.start();
        } catch (IOException exception) {
            throw new DesktopBackupException("Unable to start " + command.get(0) + ".", exception);
        }
    }

    private static int waitFor(Process process) {
        try {
            boolean finished = process.waitFor(COMMAND_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new DesktopBackupException("The database tool timed out.");
            }
            return process.exitValue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new DesktopBackupException("The database tool was interrupted.", exception);
        }
    }

    private static String host(Environment environment) {
        return firstNonBlank(
            environment.getProperty("DB_HOST"),
            environment.getProperty("app.desktop.db-host"),
            "127.0.0.1"
        );
    }

    private static int port(Environment environment) {
        String raw = firstNonBlank(
            environment.getProperty("DB_PORT"),
            environment.getProperty("app.desktop.db-port")
        );
        if (raw == null) {
            return 54329;
        }
        return Integer.parseInt(raw);
    }

    private static String username(Environment environment) {
        return firstNonBlank(
            environment.getProperty("DB_USERNAME"),
            environment.getProperty("spring.datasource.username"),
            "inventory_user"
        );
    }

    private static String password(Environment environment) {
        String password = firstNonBlank(
            environment.getProperty("DB_PASSWORD"),
            environment.getProperty("spring.datasource.password")
        );
        return password == null ? "" : password;
    }

    static String databaseName(Environment environment) {
        String url = environment.getProperty("spring.datasource.url", "");
        int slash = url.lastIndexOf('/');
        if (slash >= 0 && slash < url.length() - 1) {
            String name = url.substring(slash + 1);
            int query = name.indexOf('?');
            return query >= 0 ? name.substring(0, query) : name;
        }
        return firstNonBlank(environment.getProperty("POSTGRES_DB"), "inventory_management");
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank() && !(value.startsWith("${") && value.endsWith("}"))) {
                return value.trim();
            }
        }
        return null;
    }
}
