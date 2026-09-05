package com.inventory.desktop;

public record DesktopRestoreResponse(boolean restartRequired, String message) {
}
