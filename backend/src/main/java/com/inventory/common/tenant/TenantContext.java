package com.inventory.common.tenant;

import java.util.Optional;
import java.util.UUID;

import com.inventory.auth.entity.MembershipRole;

public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_BUSINESS_ID = new ThreadLocal<>();
    private static final ThreadLocal<MembershipRole> CURRENT_ROLE = new ThreadLocal<>();
    private static final ThreadLocal<UUID> CURRENT_USER_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static Optional<UUID> getBusinessId() {
        return Optional.ofNullable(CURRENT_BUSINESS_ID.get());
    }

    public static Optional<MembershipRole> getRole() {
        return Optional.ofNullable(CURRENT_ROLE.get());
    }

    public static Optional<UUID> getUserId() {
        return Optional.ofNullable(CURRENT_USER_ID.get());
    }

    public static void set(UUID businessId, MembershipRole role, UUID userId) {
        CURRENT_BUSINESS_ID.set(businessId);
        CURRENT_ROLE.set(role);
        CURRENT_USER_ID.set(userId);
    }

    public static void setBusinessId(UUID businessId) {
        CURRENT_BUSINESS_ID.set(businessId);
    }

    public static void clear() {
        CURRENT_BUSINESS_ID.remove();
        CURRENT_ROLE.remove();
        CURRENT_USER_ID.remove();
    }
}
