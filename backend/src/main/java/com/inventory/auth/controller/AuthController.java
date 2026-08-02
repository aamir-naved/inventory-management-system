package com.inventory.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.inventory.auth.dto.AuthResponse;
import com.inventory.auth.dto.ForgotPasswordRequest;
import com.inventory.auth.dto.LoginRequest;
import com.inventory.auth.dto.LogoutRequest;
import com.inventory.auth.dto.MessageResponse;
import com.inventory.auth.dto.RefreshTokenRequest;
import com.inventory.auth.dto.RegisterRequest;
import com.inventory.auth.dto.ResetPasswordRequest;
import com.inventory.auth.dto.UpdateProfileRequest;
import com.inventory.auth.dto.VerifyEmailRequest;
import com.inventory.auth.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AuthResponse currentSession() {
        return authService.currentSession();
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    @PostMapping("/verify-email")
    public MessageResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return authService.verifyEmail(request);
    }

    @PostMapping("/resend-verification")
    public MessageResponse resendVerification() {
        return authService.resendVerification();
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    public MessageResponse logout(@RequestBody(required = false) LogoutRequest request) {
        return authService.logout(request == null ? new LogoutRequest(null) : request);
    }

    @PatchMapping("/profile")
    public AuthResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(request);
    }
}
