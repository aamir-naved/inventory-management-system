package com.inventory.sales.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SaleUpdateRequest(
    LocalDate saleDate,
    @Size(max = 255, message = "Notes must be 255 characters or fewer")
    String notes
) {}
