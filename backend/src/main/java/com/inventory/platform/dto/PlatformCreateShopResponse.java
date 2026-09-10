package com.inventory.platform.dto;

public record PlatformCreateShopResponse(
    PlatformShopDetailResponse shop,
    String passwordDelivery,
    String message
) {
}
