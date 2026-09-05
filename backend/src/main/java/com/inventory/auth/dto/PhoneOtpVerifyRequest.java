package com.inventory.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PhoneOtpVerifyRequest(
    @NotBlank(message = "Mobile number is required")
    String phone,

    @NotBlank(message = "Enter the 6-digit code")
    @Size(min = 4, max = 8, message = "Enter the 6-digit code")
    String code
) {
}
