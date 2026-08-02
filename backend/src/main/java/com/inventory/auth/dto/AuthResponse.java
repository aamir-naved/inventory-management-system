package com.inventory.auth.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuthResponse(
    String accessToken,
    String tokenType,
    OffsetDateTime expiresAt,
    String refreshToken,
    OffsetDateTime refreshExpiresAt,
    SessionUserResponse user
) {
    public record SessionUserResponse(
        UUID userId,
        String fullName,
        String email,
        boolean emailVerified,
        UUID businessId,
        String businessName
    ) {
    }
}
