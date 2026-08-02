package com.inventory.sales.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaleCancellationRequest(
    @NotBlank(message = "Cancellation reason is required")
    @Size(max = 255, message = "Cancellation reason must be 255 characters or fewer")
    String reason
) {}
