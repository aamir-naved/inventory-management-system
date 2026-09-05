package com.inventory.platform.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PlatformShopSummaryResponse(
    UUID id,
    String name,
    String mobileNumber,
    boolean active,
    String planCode,
    String ownerName,
    String ownerEmail,
    OffsetDateTime createdAt
) {
}
