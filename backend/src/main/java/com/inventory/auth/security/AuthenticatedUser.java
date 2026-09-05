package com.inventory.auth.security;

import java.util.UUID;

public record AuthenticatedUser(
    UUID userId,
    String email,
    boolean platformAdmin
) {
}
