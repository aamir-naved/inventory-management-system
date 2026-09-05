package com.inventory.staff.dto;

import java.time.OffsetDateTime;

public record StaffInvitePreviewResponse(
    String businessName,
    String email,
    String role,
    OffsetDateTime expiresAt
) {
}
