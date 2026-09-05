package com.inventory.notification.dto;

import java.util.List;

public record NotificationListResponse(
    int count,
    List<NotificationItem> items
) {
}
