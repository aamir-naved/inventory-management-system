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
import com.inventory.auth.dto.PhoneOtpRequest;
import com.inventory.auth.dto.PhoneOtpVerifyRequest;
import com.inventory.auth.dto.PublicConfigResponse;
import com.inventory.auth.service.AuthService;
import com.inventory.auth.service.OtpAuthService;
import com.inventory.config.DesktopProperties;
import com.inventory.config.RegistrationPolicy;
import com.inventory.staff.dto.AcceptInviteRequest;
import com.inventory.staff.dto.StaffInvitePreviewResponse;
import com.inventory.staff.service.StaffService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final OtpAuthService otpAuthService;
    private final StaffService staffService;
    private final RegistrationPolicy registrationPolicy;
    private final DesktopProperties desktopProperties;

    public AuthController(
        AuthService authService,
        OtpAuthService otpAuthService,
        StaffService staffService,
        RegistrationPolicy registrationPolicy,
        DesktopProperties desktopProperties
    ) {
        this.authService = authService;
        this.otpAuthService = otpAuthService;
        this.staffService = staffService;
        this.registrationPolicy = registrationPolicy;
        this.desktopProperties = desktopProperties;
    }

    @GetMapping("/public-config")
    public PublicConfigResponse publicConfig() {
        return new PublicConfigResponse(registrationPolicy.isOpen(), desktopProperties.isEnabled());
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/otp/request")
    public MessageResponse requestOtp(@Valid @RequestBody PhoneOtpRequest request) {
        return otpAuthService.requestCode(request.phone());
    }

    @PostMapping("/otp/verify")
    public AuthResponse verifyOtp(@Valid @RequestBody PhoneOtpVerifyRequest request) {
        return otpAuthService.verify(request.phone(), request.code());
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

    @GetMapping("/invite")
    public StaffInvitePreviewResponse previewInvite(@RequestParam String token) {
        return staffService.preview(token);
    }

    @PostMapping("/accept-invite")
    public AuthResponse acceptInvite(@Valid @RequestBody AcceptInviteRequest request) {
        return staffService.accept(request);
    }
}
