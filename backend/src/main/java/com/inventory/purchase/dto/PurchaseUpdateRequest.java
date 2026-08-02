package com.inventory.purchase.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PurchaseUpdateRequest(
    LocalDate purchaseDate,

    @Size(max = 255, message = "Notes must be 255 characters or fewer")
    String notes
) {
}
