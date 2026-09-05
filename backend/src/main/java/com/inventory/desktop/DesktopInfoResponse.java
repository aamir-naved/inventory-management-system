package com.inventory.desktop;

public record DesktopInfoResponse(boolean desktop, String dataDir, boolean pendingRestore) {
}
