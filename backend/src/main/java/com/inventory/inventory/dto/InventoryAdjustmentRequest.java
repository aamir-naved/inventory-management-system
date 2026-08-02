package com.inventory.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record InventoryAdjustmentRequest(
    @NotNull(message = "Product ID is required")
    UUID productId,

    @NotNull(message = "Adjustment quantity is required")
    @Digits(integer = 16, fraction = 3, message = "Adjustment quantity must have up to 3 decimal places")
    BigDecimal adjustmentQuantity,

    @NotBlank(message = "Reason is required")
    @Size(max = 255, message = "Reason must be 255 characters or fewer")
    String reason
) {
}
