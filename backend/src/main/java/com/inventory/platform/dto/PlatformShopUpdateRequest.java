package com.inventory.platform.dto;

import jakarta.validation.constraints.Size;

public record PlatformShopUpdateRequest(
    Boolean active,
    @Size(max = 255, message = "Suspended reason must be 255 characters or fewer")
    String suspendedReason
) {
}
