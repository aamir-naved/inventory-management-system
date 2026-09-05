package com.inventory.auth.entity;

public final class PlatformRole {

    public static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";

    private PlatformRole() {
    }

    public static boolean isPlatformAdmin(String value) {
        return PLATFORM_ADMIN.equals(value);
    }
}
