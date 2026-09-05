package com.inventory.business.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BusinessResponse(
    UUID id,
    String name,
    String businessType,
    String addressLine,
    String mobileNumber,
    String currencyCode,
    String timeZone,
    boolean gstEnabled,
    String gstin,
    String stateCode,
    String stateName,
    boolean gstInclusivePricing,
    boolean hasLogo,
    boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
