package com.inventory.platform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlatformCreateShopRequest(
    @NotBlank(message = "Shop name is required")
    @Size(max = 150, message = "Shop name must be 150 characters or fewer")
    String shopName,

    @NotBlank(message = "Owner name is required")
    @Size(max = 120, message = "Owner name must be 120 characters or fewer")
    String ownerName,

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email")
    String email,

    @NotBlank(message = "Mobile number is required")
    String phone,

    @Size(min = 8, max = 100, message = "Temporary password must be 8 to 100 characters")
    String temporaryPassword,

    Boolean provisionStarterCatalog
) {
}
