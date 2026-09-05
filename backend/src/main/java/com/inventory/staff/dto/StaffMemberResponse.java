package com.inventory.staff.dto;

import java.util.UUID;

public record StaffMemberResponse(
    UUID membershipId,
    UUID userId,
    String fullName,
    String email,
    String role,
    boolean active
) {
}
