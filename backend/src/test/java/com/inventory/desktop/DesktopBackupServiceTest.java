package com.inventory.desktop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import com.inventory.config.DesktopProperties;

class DesktopBackupServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void queuesGzipRestoreForNextStart() throws Exception {
        DesktopProperties properties = new DesktopProperties();
        properties.setEnabled(true);
        properties.setDataDir(tempDir.toString());
        DesktopBackupService service = new DesktopBackupService(properties, new MockEnvironment());

        byte[] payload = new byte[32];
        payload[0] = 0x1f;
        payload[1] = (byte) 0x8b;
        service.queueRestore(new ByteArrayInputStream(payload), "shop.sql.gz");

        assertThat(service.pendingRestoreQueued()).isTrue();
        assertThat(Files.size(service.pendingRestoreFile())).isEqualTo(32);
    }

    @Test
    void rejectsNonGzipRestore() {
        DesktopProperties properties = new DesktopProperties();
        properties.setDataDir(tempDir.toString());
        DesktopBackupService service = new DesktopBackupService(properties, new MockEnvironment());

        assertThatThrownBy(() -> service.queueRestore(new ByteArrayInputStream(new byte[32]), "shop.sql"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(".sql.gz");
    }
}
