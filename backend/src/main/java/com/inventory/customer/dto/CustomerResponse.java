package com.inventory.customer.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CustomerResponse(
    UUID id,
    UUID businessId,
    String name,
    String contactPerson,
    String mobileNumber,
    String addressLine,
    String stateCode,
    boolean archived,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
