package com.inventory.desktop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

class DesktopPostgresCliTest {

    @TempDir
    Path tempDir;

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void dumpsThroughConfiguredPgBin() throws Exception {
        Path bin = tempDir.resolve("bin");
        Files.createDirectories(bin);
        Path pgDump = bin.resolve("pg_dump");
        Files.writeString(pgDump, """
            #!/bin/sh
            echo '-- dump'
            """);
        Files.setPosixFilePermissions(pgDump, PosixFilePermissions.fromString("rwxr-xr-x"));

        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("APP_DESKTOP_PG_BIN", bin.toString());
        environment.setProperty("DB_HOST", "127.0.0.1");
        environment.setProperty("DB_PORT", "54329");
        environment.setProperty("DB_USERNAME", "inventory_user");
        environment.setProperty("DB_PASSWORD", "secret");
        environment.setProperty("spring.datasource.url", "jdbc:postgresql://127.0.0.1:54329/inventory_management");

        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        DesktopPostgresCli.writeGzipDump(environment, output);
        assertThat(output.size()).isGreaterThan(10);
    }

    @Test
    void restoreRejectsMissingTools() throws Exception {
        Path backup = tempDir.resolve("restore-pending.sql.gz");
        try (GZIPOutputStream gzip = new GZIPOutputStream(Files.newOutputStream(backup))) {
            gzip.write("select 1;".getBytes());
        }
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("APP_DESKTOP_PG_BIN", tempDir.resolve("missing").toString());

        assertThatThrownBy(() -> DesktopPostgresCli.restoreGzipSql(environment, backup))
            .isInstanceOf(DesktopBackupException.class)
            .hasMessageContaining("PostgreSQL tools were not found");
    }
}
