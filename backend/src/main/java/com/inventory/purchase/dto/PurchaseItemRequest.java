package com.inventory.purchase.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseItemRequest(
    @NotNull(message = "Product ID is required")
    UUID productId,

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.001", inclusive = true, message = "Quantity must be greater than zero")
    @Digits(integer = 16, fraction = 3, message = "Quantity must have up to 3 decimal places")
    BigDecimal quantity,

    @NotNull(message = "Purchase price is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Purchase price cannot be negative")
    @Digits(integer = 17, fraction = 2, message = "Purchase price must have up to 2 decimal places")
    BigDecimal purchasePrice,

    @DecimalMin(value = "0.00", inclusive = true, message = "GST rate cannot be negative")
    @Digits(integer = 3, fraction = 2, message = "GST rate must have up to 2 decimal places")
    BigDecimal gstRate
) {
}
