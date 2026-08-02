package com.inventory.supplier.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record SupplierRequest(
    @NotBlank(message = "Supplier name is required")
    @Size(max = 150, message = "Supplier name must be 150 characters or fewer")
    String name,

    @Size(max = 120, message = "Contact person must be 120 characters or fewer")
    String contactPerson,

    @Pattern(
        regexp = "^$|^[0-9+\\-() ]{7,20}$",
        message = "Mobile number must be 7 to 20 valid phone characters"
    )
    String mobileNumber,

    @Size(max = 255, message = "Address must be 255 characters or fewer")
    String addressLine
) {
}
