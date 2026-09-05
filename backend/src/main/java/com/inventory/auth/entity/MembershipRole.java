package com.inventory.auth.entity;

public enum MembershipRole {
    CLERK,
    MANAGER,
    OWNER;

    public boolean atLeast(MembershipRole required) {
        return ordinal() >= required.ordinal();
    }

    public static MembershipRole from(String value) {
        if (value == null || value.isBlank()) {
            return CLERK;
        }
        try {
            return MembershipRole.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return CLERK;
        }
    }

    public boolean isStaffInviteRole() {
        return this == MANAGER || this == CLERK;
    }
}
