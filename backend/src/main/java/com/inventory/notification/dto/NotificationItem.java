package com.inventory.notification.dto;

import java.util.UUID;

public record NotificationItem(
    String type,
    String title,
    String detail,
    UUID entityId
) {
}
