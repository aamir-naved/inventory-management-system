package com.inventory.purchase.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PurchaseRequest(
    @NotNull(message = "Supplier ID is required")
    UUID supplierId,

    @NotNull(message = "Purchase date is required")
    LocalDate purchaseDate,

    @DecimalMin(value = "0.00", message = "Amount paid cannot be negative")
    BigDecimal amountPaid,

    @Size(max = 255, message = "Notes must be 255 characters or fewer")
    String notes,

    /** Ignored; interstate is derived from party vs business state codes. */
    Boolean interstate,

    @NotEmpty(message = "At least one purchase item is required")
    List<@Valid PurchaseItemRequest> items
) {
}
