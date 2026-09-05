package com.inventory.desktop;

public class DesktopBackupException extends RuntimeException {

    public DesktopBackupException(String message) {
        super(message);
    }

    public DesktopBackupException(String message, Throwable cause) {
        super(message, cause);
    }
}
