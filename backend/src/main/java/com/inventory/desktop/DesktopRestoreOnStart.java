package com.inventory.desktop;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

public class DesktopRestoreOnStart implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        if (!DesktopEnvironmentPostProcessor.isDesktopProfile(environment)) {
            return;
        }
        Path dataDir = DesktopPaths.resolveDataDir(environment.getProperty("app.desktop.data-dir"));
        Path pending = dataDir.resolve(DesktopPaths.PENDING_RESTORE_FILE);
        if (!Files.exists(pending)) {
            return;
        }
        DesktopPostgresCli.restoreGzipSql(environment, pending);
        try {
            Files.deleteIfExists(pending);
        } catch (Exception exception) {
            throw new DesktopBackupException("Restore finished but the pending file could not be removed.", exception);
        }
    }
}
