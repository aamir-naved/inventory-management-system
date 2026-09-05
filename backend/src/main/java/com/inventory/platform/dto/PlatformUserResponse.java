package com.inventory.platform.dto;

import java.util.UUID;

public record PlatformUserResponse(
    UUID id,
    String fullName,
    String email,
    String phone,
    boolean active,
    String platformRole,
    UUID shopId,
    String shopName,
    String membershipRole
) {
}
