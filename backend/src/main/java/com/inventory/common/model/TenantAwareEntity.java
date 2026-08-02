package com.inventory.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.util.UUID;

@MappedSuperclass
public abstract class TenantAwareEntity extends AuditableEntity implements BusinessScoped {

    @Column(name = "business_id", nullable = false, updatable = false)
    private UUID businessId;

    @Override
    public UUID getBusinessId() {
        return businessId;
    }

    public void setBusinessId(UUID businessId) {
        this.businessId = businessId;
    }
}
