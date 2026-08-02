package com.inventory.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank(message = "Full name is required")
    @Size(max = 120, message = "Full name must be at most 120 characters")
    String fullName,

    String currentPassword,

    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    String newPassword
) {
}
