package com.inventory.settings.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SettingsResponse(
    UUID businessId,
    String currencyCode,
    String dateFormat,
    boolean allowNegativeStock,
    BigDecimal defaultLowStockThreshold
) {
}
