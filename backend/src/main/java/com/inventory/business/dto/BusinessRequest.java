package com.inventory.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BusinessRequest(
    @NotBlank(message = "Business name is required")
    @Size(max = 150, message = "Business name must be 150 characters or fewer")
    String name,

    @NotBlank(message = "Business type is required")
    @Size(max = 100, message = "Business type must be 100 characters or fewer")
    String businessType,

    @Size(max = 255, message = "Address must be 255 characters or fewer")
    String addressLine,

    @NotBlank(message = "Mobile number is required")
    @Pattern(
        regexp = "^[0-9+\\-() ]{7,20}$",
        message = "Mobile number must be 7 to 20 valid phone characters"
    )
    String mobileNumber,

    @NotBlank(message = "Currency code is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be a 3-letter uppercase code")
    String currencyCode,

    @NotBlank(message = "Time zone is required")
    @Size(max = 50, message = "Time zone must be 50 characters or fewer")
    String timeZone,

    Boolean gstEnabled,

    @Size(max = 15, message = "GSTIN must be 15 characters or fewer")
    String gstin,

    @Size(max = 2, message = "State code must be 2 characters")
    String stateCode,

    @Size(max = 100, message = "State name must be 100 characters or fewer")
    String stateName,

    Boolean gstInclusivePricing
) {
}
