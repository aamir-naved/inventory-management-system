package com.inventory.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record QuickStartRequest(
    @NotBlank(message = "Shop name is required")
    @Size(max = 150, message = "Shop name must be 150 characters or fewer")
    String shopName,

    @NotBlank(message = "Mobile number is required")
    @Pattern(
        regexp = "^[0-9+\\-() ]{7,20}$",
        message = "Mobile number must be 7 to 20 valid phone characters"
    )
    String mobileNumber
) {
}
