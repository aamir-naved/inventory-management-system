package com.inventory.desktop;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/desktop")
@ConditionalOnProperty(prefix = "app.desktop", name = "enabled", havingValue = "true")
public class DesktopBackupController {

    private final DesktopBackupOperations desktopBackupService;

    public DesktopBackupController(DesktopBackupOperations desktopBackupService) {
        this.desktopBackupService = desktopBackupService;
    }

    @GetMapping("/info")
    public DesktopInfoResponse info() {
        return new DesktopInfoResponse(
            true,
            desktopBackupService.dataDir().toString(),
            desktopBackupService.pendingRestoreQueued()
        );
    }

    @GetMapping("/backup")
    public ResponseEntity<byte[]> exportBackup() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        desktopBackupService.writeGzipDump(buffer);
        String filename = "inventory-backup-"
            + DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").format(OffsetDateTime.now(ZoneOffset.UTC))
            + ".sql.gz";
        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString()
            )
            .header(HttpHeaders.CONTENT_TYPE, "application/gzip")
            .body(buffer.toByteArray());
    }

    @PostMapping(value = "/restore", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DesktopRestoreResponse restore(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose a .sql.gz backup exported from this app.");
        }
        desktopBackupService.queueRestore(file.getInputStream(), file.getOriginalFilename());
        return new DesktopRestoreResponse(
            true,
            "The backup will be applied the next time you open this app. Close the window, then open it again."
        );
    }
}
