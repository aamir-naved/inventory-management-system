package com.inventory.auth.service;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.auth.dto.AuthResponse;
import com.inventory.auth.dto.LoginRequest;
import com.inventory.auth.dto.RegisterRequest;
import com.inventory.auth.entity.BusinessMembership;
import com.inventory.auth.entity.UserAccount;
import com.inventory.auth.repository.BusinessMembershipRepository;
import com.inventory.auth.repository.UserAccountRepository;
import com.inventory.auth.security.CurrentUser;
import com.inventory.auth.security.JwtService;

@Service
@Transactional
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final BusinessMembershipRepository businessMembershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUser currentUser;

    public AuthService(
        UserAccountRepository userAccountRepository,
        BusinessMembershipRepository businessMembershipRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        CurrentUser currentUser
    ) {
        this.userAccountRepository = userAccountRepository;
        this.businessMembershipRepository = businessMembershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currentUser = currentUser;
    }

    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        if (userAccountRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        UserAccount userAccount = new UserAccount();
        userAccount.setFullName(request.fullName().trim());
        userAccount.setEmail(normalizedEmail);
        userAccount.setPasswordHash(passwordEncoder.encode(request.password()));
        userAccount.setEmailVerified(true);
        userAccount.setActive(true);

        UserAccount savedUser = userAccountRepository.save(userAccount);
        return buildAuthResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserAccount userAccount = userAccountRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!userAccount.isActive() || !passwordEncoder.matches(request.password(), userAccount.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        return buildAuthResponse(userAccount);
    }

    @Transactional(readOnly = true)
    public AuthResponse currentSession() {
        UUID userId = currentUser.requireUserId();
        UserAccount userAccount = userAccountRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        return buildAuthResponse(userAccount);
    }

    private AuthResponse buildAuthResponse(UserAccount userAccount) {
        JwtService.JwtToken token = jwtService.issueToken(userAccount.getId(), userAccount.getEmail());
        Optional<BusinessMembership> activeMembership = businessMembershipRepository.findAllByUser_Id(userAccount.getId())
            .stream()
            .filter(BusinessMembership::isActive)
            .max(Comparator.comparing(BusinessMembership::getCreatedAt));

        AuthResponse.SessionUserResponse sessionUser = new AuthResponse.SessionUserResponse(
            userAccount.getId(),
            userAccount.getFullName(),
            userAccount.getEmail(),
            activeMembership.map(membership -> membership.getBusiness().getId()).orElse(null),
            activeMembership.map(membership -> membership.getBusiness().getName()).orElse(null)
        );

        return new AuthResponse(token.value(), "Bearer", token.expiresAt(), sessionUser);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
