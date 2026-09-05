package com.inventory.staff.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record StaffInviteRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "MANAGER|CLERK", message = "Role must be MANAGER or CLERK")
    String role
) {
}
