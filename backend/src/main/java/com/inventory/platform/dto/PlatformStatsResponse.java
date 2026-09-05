package com.inventory.platform.dto;

import java.math.BigDecimal;

public record PlatformStatsResponse(
    long shopCount,
    long activeShopCount,
    long suspendedShopCount,
    long userCount,
    long todaysSaleCount,
    BigDecimal todaysSaleAmount
) {
}
