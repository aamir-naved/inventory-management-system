package com.inventory.auth.service;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
import com.inventory.auth.entity.AuthToken;
import com.inventory.auth.entity.AuthTokenType;
import com.inventory.auth.entity.BusinessMembership;
import com.inventory.auth.entity.UserAccount;
import com.inventory.auth.entity.PlatformRole;
import com.inventory.auth.mail.MailMessage;
import com.inventory.auth.mail.MailService;
import com.inventory.auth.repository.BusinessMembershipRepository;
import com.inventory.auth.repository.UserAccountRepository;
import com.inventory.auth.security.CurrentUser;
import com.inventory.auth.security.JwtService;
import com.inventory.config.AuthProperties;
import com.inventory.config.RegistrationPolicy;

@Service
@Transactional
public class AuthService {

    private static final String FORGOT_PASSWORD_MESSAGE =
        "If an account exists for that email, password reset instructions have been sent.";

    private final UserAccountRepository userAccountRepository;
    private final BusinessMembershipRepository businessMembershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUser currentUser;
    private final AuthTokenService authTokenService;
    private final MailService mailService;
    private final AuthProperties authProperties;
    private final RegistrationPolicy registrationPolicy;

    public AuthService(
        UserAccountRepository userAccountRepository,
        BusinessMembershipRepository businessMembershipRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        CurrentUser currentUser,
        AuthTokenService authTokenService,
        MailService mailService,
        AuthProperties authProperties,
        RegistrationPolicy registrationPolicy
    ) {
        this.userAccountRepository = userAccountRepository;
        this.businessMembershipRepository = businessMembershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currentUser = currentUser;
        this.authTokenService = authTokenService;
        this.mailService = mailService;
        this.authProperties = authProperties;
        this.registrationPolicy = registrationPolicy;
    }

    public AuthResponse register(RegisterRequest request) {
        registrationPolicy.requireOpenSelfServe("Public registration is disabled. Ask the operator to create your shop.");
        String normalizedEmail = normalizeEmail(request.email());
        if (userAccountRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        UserAccount userAccount = new UserAccount();
        userAccount.setFullName(request.fullName().trim());
        userAccount.setEmail(normalizedEmail);
        userAccount.setPasswordHash(passwordEncoder.encode(request.password()));
        userAccount.setEmailVerified(false);
        userAccount.setActive(true);

        UserAccount savedUser = userAccountRepository.saveAndFlush(userAccount);
        sendVerificationEmail(savedUser);
        return buildAuthResponse(savedUser, true);
    }

    public AuthResponse login(LoginRequest request) {
        UserAccount userAccount = userAccountRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!userAccount.isActive() || !passwordEncoder.matches(request.password(), userAccount.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        return buildAuthResponse(userAccount, true);
    }

    public AuthResponse currentSession() {
        return buildAuthResponse(requireCurrentUser(), false);
    }

    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        userAccountRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
            .filter(UserAccount::isActive)
            .ifPresent(this::sendPasswordResetEmail);

        return new MessageResponse(FORGOT_PASSWORD_MESSAGE);
    }

    public MessageResponse resetPassword(ResetPasswordRequest request) {
        AuthToken authToken = authTokenService.requireActiveToken(request.token(), AuthTokenType.PASSWORD_RESET);
        UserAccount userAccount = authToken.getUser();
        userAccount.setPasswordHash(passwordEncoder.encode(request.password()));
        userAccountRepository.save(userAccount);
        authTokenService.markUsed(authToken);
        authTokenService.revokeSessions(userAccount);
        return new MessageResponse("Password has been reset. You can sign in with your new password.");
    }

    public MessageResponse verifyEmail(VerifyEmailRequest request) {
        AuthToken authToken = authTokenService.requireActiveToken(request.token(), AuthTokenType.EMAIL_VERIFY);
        UserAccount userAccount = authToken.getUser();
        userAccount.setEmailVerified(true);
        userAccountRepository.save(userAccount);
        authTokenService.markUsed(authToken);
        return new MessageResponse("Email verified successfully.");
    }

    public MessageResponse resendVerification() {
        UserAccount userAccount = requireCurrentUser();
        if (userAccount.isEmailVerified()) {
            return new MessageResponse("Email is already verified.");
        }

        sendVerificationEmail(userAccount);
        return new MessageResponse("Verification email sent.");
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        AuthToken authToken = authTokenService.requireActiveToken(request.refreshToken(), AuthTokenType.REFRESH);
        UserAccount userAccount = authToken.getUser();
        if (!userAccount.isActive()) {
            throw new IllegalArgumentException("Invalid or expired token");
        }

        authTokenService.markUsed(authToken);
        return buildAuthResponse(userAccount, true);
    }

    public MessageResponse logout(LogoutRequest request) {
        authTokenService.revokeSessions(requireCurrentUser());
        return new MessageResponse("Signed out.");
    }

    public AuthResponse updateProfile(UpdateProfileRequest request) {
        UserAccount userAccount = requireCurrentUser();
        userAccount.setFullName(request.fullName().trim());

        boolean changingPassword = StringUtils.hasText(request.newPassword());
        if (changingPassword) {
            if (!StringUtils.hasText(request.currentPassword())
                || !passwordEncoder.matches(request.currentPassword(), userAccount.getPasswordHash())) {
                throw new IllegalArgumentException("Current password is incorrect");
            }
            userAccount.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        }

        userAccountRepository.saveAndFlush(userAccount);

        if (changingPassword) {
            authTokenService.revokeSessions(userAccount);
        }

        return buildAuthResponse(userAccount, changingPassword);
    }

    public AuthResponse issueSession(UserAccount userAccount, boolean includeRefreshToken) {
        return buildAuthResponse(userAccount, includeRefreshToken);
    }

    private UserAccount requireCurrentUser() {
        UUID userId = currentUser.requireUserId();
        return userAccountRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
    }

    private void sendVerificationEmail(UserAccount userAccount) {
        AuthTokenService.IssuedToken issuedToken =
            authTokenService.issueToken(userAccount, AuthTokenType.EMAIL_VERIFY);
        String link = appLink("/verify-email?token=" + issuedToken.rawToken());
        mailService.send(new MailMessage(
            userAccount.getEmail(),
            "Verify your email",
            "Welcome to Inventory Management.\n\nVerify your email using this link:\n" + link + "\n\n"
                + "If you did not create an account, you can ignore this message."
        ));
    }

    private void sendPasswordResetEmail(UserAccount userAccount) {
        AuthTokenService.IssuedToken issuedToken =
            authTokenService.issueToken(userAccount, AuthTokenType.PASSWORD_RESET);
        String link = appLink("/reset-password?token=" + issuedToken.rawToken());
        mailService.send(new MailMessage(
            userAccount.getEmail(),
            "Reset your password",
            "Reset your password using this link:\n" + link + "\n\n"
                + "If you did not request a reset, you can ignore this message."
        ));
    }

    private String appLink(String path) {
        String base = authProperties.getPublicAppUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + path;
    }

    private AuthResponse buildAuthResponse(UserAccount userAccount, boolean includeRefreshToken) {
        JwtService.JwtToken accessToken = jwtService.issueToken(
            userAccount.getId(),
            userAccount.getEmail() != null ? userAccount.getEmail() : userAccount.getPhone(),
            userAccount.getTokenVersion()
        );
        boolean platformAdmin = userAccount.isPlatformAdmin();
        Optional<BusinessMembership> activeMembership = platformAdmin
            ? Optional.empty()
            : businessMembershipRepository.findAllByUser_Id(userAccount.getId())
                .stream()
                .filter(BusinessMembership::isActive)
                .max(Comparator.comparing(BusinessMembership::getCreatedAt));

        AuthResponse.SessionUserResponse sessionUser = new AuthResponse.SessionUserResponse(
            userAccount.getId(),
            userAccount.getFullName(),
            userAccount.getEmail(),
            userAccount.getPhone(),
            userAccount.isEmailVerified(),
            activeMembership.map(membership -> membership.getBusiness().getId()).orElse(null),
            activeMembership.map(membership -> membership.getBusiness().getName()).orElse(null),
            activeMembership.map(BusinessMembership::getRole).orElse(null),
            platformAdmin ? PlatformRole.PLATFORM_ADMIN : null
        );

        String refreshToken = null;
        java.time.OffsetDateTime refreshExpiresAt = null;
        if (includeRefreshToken) {
            AuthTokenService.IssuedToken issuedRefresh =
                authTokenService.issueToken(userAccount, AuthTokenType.REFRESH);
            refreshToken = issuedRefresh.rawToken();
            refreshExpiresAt = issuedRefresh.expiresAt();
        }

        return new AuthResponse(
            accessToken.value(),
            "Bearer",
            accessToken.expiresAt(),
            refreshToken,
            refreshExpiresAt,
            sessionUser
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
