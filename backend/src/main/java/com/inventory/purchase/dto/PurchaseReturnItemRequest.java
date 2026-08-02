package com.inventory.purchase.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseReturnItemRequest(
    @NotNull(message = "Purchase item ID is required")
    UUID purchaseItemId,
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.001", inclusive = true, message = "Quantity must be greater than zero")
    @Digits(integer = 16, fraction = 3, message = "Quantity must have up to 3 decimal places")
    BigDecimal quantity
) {
}
