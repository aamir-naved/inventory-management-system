package com.inventory.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record PhoneOtpRequest(
    @NotBlank(message = "Mobile number is required")
    String phone
) {
}
