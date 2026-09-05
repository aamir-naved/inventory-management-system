package com.inventory.settings.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SettingsRequest(
    @NotBlank(message = "Currency code is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be a 3-letter uppercase code")
    String currencyCode,

    @NotBlank(message = "Date format is required")
    @Size(max = 32, message = "Date format must be 32 characters or fewer")
    String dateFormat,

    @NotNull(message = "Negative stock setting is required")
    Boolean allowNegativeStock,

    @NotNull(message = "Default low stock threshold is required")
    @DecimalMin(value = "0.000", inclusive = true, message = "Default low stock threshold cannot be negative")
    @Digits(integer = 16, fraction = 3, message = "Default low stock threshold must have up to 3 decimal places")
    BigDecimal defaultLowStockThreshold,

    Boolean gstEnabled,

    @Size(max = 15, message = "GSTIN must be 15 characters or fewer")
    String gstin,

    @Size(max = 2, message = "State code must be 2 characters")
    String stateCode,

    @Size(max = 100, message = "State name must be 100 characters or fewer")
    String stateName,

    Boolean gstInclusivePricing
) {
}
