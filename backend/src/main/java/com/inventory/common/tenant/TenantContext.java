package com.inventory.common.tenant;

import java.util.Optional;
import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_BUSINESS_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static Optional<UUID> getBusinessId() {
        return Optional.ofNullable(CURRENT_BUSINESS_ID.get());
    }

    public static void setBusinessId(UUID businessId) {
        CURRENT_BUSINESS_ID.set(businessId);
    }

    public static void clear() {
        CURRENT_BUSINESS_ID.remove();
    }
}
