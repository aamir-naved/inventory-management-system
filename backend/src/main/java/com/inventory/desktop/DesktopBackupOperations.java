package com.inventory.desktop;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

public interface DesktopBackupOperations {

    Path dataDir();

    boolean pendingRestoreQueued();

    void writeGzipDump(OutputStream output);

    void queueRestore(InputStream gzipSql, String originalFilename);
}
