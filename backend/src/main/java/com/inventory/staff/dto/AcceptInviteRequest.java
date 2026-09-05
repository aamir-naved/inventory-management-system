package com.inventory.staff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInviteRequest(
    @NotBlank(message = "Invite token is required")
    String token,

    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name must be 150 characters or fewer")
    String fullName,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be 8 to 100 characters")
    String password
) {
}
