package com.inventory.supplier.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SupplierResponse(
    UUID id,
    UUID businessId,
    String name,
    String contactPerson,
    String mobileNumber,
    String addressLine,
    boolean archived,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
