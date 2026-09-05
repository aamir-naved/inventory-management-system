package com.inventory.platform.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PlatformShopDetailResponse(
    UUID id,
    String name,
    String mobileNumber,
    boolean active,
    String suspendedReason,
    String planCode,
    String ownerName,
    String ownerEmail,
    String ownerPhone,
    UUID ownerUserId,
    long staffCount,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
