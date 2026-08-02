package com.inventory.auth.dto;

public record LogoutRequest(
    String refreshToken
) {
}
