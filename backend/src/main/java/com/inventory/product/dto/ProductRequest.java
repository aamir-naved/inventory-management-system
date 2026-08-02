package com.inventory.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
    @NotBlank(message = "Product name is required")
    @Size(max = 150, message = "Product name must be 150 characters or fewer")
    String name,

    @Size(max = 60, message = "SKU must be 60 characters or fewer")
    String sku,

    @Size(max = 100, message = "Category must be 100 characters or fewer")
    String category,

    @NotBlank(message = "Unit is required")
    @Size(max = 30, message = "Unit must be 30 characters or fewer")
    String unit,

    @NotNull(message = "Cost price is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Cost price cannot be negative")
    @Digits(integer = 17, fraction = 2, message = "Cost price must have up to 2 decimal places")
    BigDecimal costPrice,

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Selling price cannot be negative")
    @Digits(integer = 17, fraction = 2, message = "Selling price must have up to 2 decimal places")
    BigDecimal sellingPrice,

    @NotNull(message = "Opening stock is required")
    @DecimalMin(value = "0.000", inclusive = true, message = "Opening stock cannot be negative")
    @Digits(integer = 16, fraction = 3, message = "Opening stock must have up to 3 decimal places")
    BigDecimal openingStock,

    @NotNull(message = "Low stock threshold is required")
    @DecimalMin(value = "0.000", inclusive = true, message = "Low stock threshold cannot be negative")
    @Digits(integer = 16, fraction = 3, message = "Low stock threshold must have up to 3 decimal places")
    BigDecimal lowStockThreshold
) {
}
