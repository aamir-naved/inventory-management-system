package com.inventory.staff.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StaffInviteResponse(
    UUID id,
    String email,
    String role,
    OffsetDateTime expiresAt,
    OffsetDateTime createdAt
) {
}
