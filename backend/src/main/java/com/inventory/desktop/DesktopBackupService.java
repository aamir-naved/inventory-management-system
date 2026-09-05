package com.inventory.desktop;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.inventory.config.DesktopProperties;

@Service
@ConditionalOnProperty(prefix = "app.desktop", name = "enabled", havingValue = "true")
public class DesktopBackupService implements DesktopBackupOperations {

    private final DesktopProperties desktopProperties;
    private final Environment environment;

    public DesktopBackupService(DesktopProperties desktopProperties, Environment environment) {
        this.desktopProperties = desktopProperties;
        this.environment = environment;
    }

    public Path dataDir() {
        return DesktopPaths.resolveDataDir(desktopProperties.getDataDir());
    }

    public Path pendingRestoreFile() {
        return dataDir().resolve(DesktopPaths.PENDING_RESTORE_FILE);
    }

    public boolean pendingRestoreQueued() {
        return Files.exists(pendingRestoreFile());
    }

    public void writeGzipDump(OutputStream output) {
        DesktopPostgresCli.writeGzipDump(environment, output);
    }

    public void queueRestore(InputStream gzipSql, String originalFilename) {
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".gz")) {
            throw new IllegalArgumentException("Choose a .sql.gz backup exported from this app.");
        }
        Path target = pendingRestoreFile();
        try {
            Files.createDirectories(target.getParent());
            try (OutputStream output = Files.newOutputStream(target)) {
                gzipSql.transferTo(output);
            }
            if (Files.size(target) < 20) {
                Files.deleteIfExists(target);
                throw new IllegalArgumentException("That backup file is empty.");
            }
        } catch (IOException exception) {
            throw new DesktopBackupException("Unable to store the backup for restore.", exception);
        }
    }
}
