package com.inventory.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentRequest(
    @NotNull(message = "Payment date is required")
    LocalDate paymentDate,

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
    BigDecimal amount,

    @Size(max = 255, message = "Notes must be 255 characters or fewer")
    String notes
) {
}
