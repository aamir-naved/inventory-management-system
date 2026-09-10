package com.inventory.platform.dto;

public record PlatformPasswordResetResponse(
    String passwordDelivery,
    String message
) {
}
