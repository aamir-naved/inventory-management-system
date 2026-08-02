package com.inventory.purchase.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record PurchaseReturnRequest(
    @NotNull(message = "Return date is required")
    LocalDate returnDate,
    @Size(max = 255, message = "Reason must be 255 characters or fewer")
    String reason,
    @Size(max = 255, message = "Notes must be 255 characters or fewer")
    String notes,
    @NotEmpty(message = "At least one return item is required")
    @Valid
    List<PurchaseReturnItemRequest> items
) {
}
