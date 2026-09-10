package com.inventory.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.auth.entity.AuthToken;
import com.inventory.auth.entity.AuthTokenType;
import com.inventory.auth.entity.UserAccount;
import com.inventory.auth.repository.AuthTokenRepository;
import com.inventory.auth.repository.UserAccountRepository;
import com.inventory.config.AuthProperties;

@Service
@Transactional
public class AuthTokenService {

    private final AuthTokenRepository authTokenRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuthProperties authProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthTokenService(
        AuthTokenRepository authTokenRepository,
        UserAccountRepository userAccountRepository,
        AuthProperties authProperties
    ) {
        this.authTokenRepository = authTokenRepository;
        this.userAccountRepository = userAccountRepository;
        this.authProperties = authProperties;
    }

    public IssuedToken issueToken(UserAccount user, AuthTokenType type) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        authTokenRepository.revokeActiveTokens(user.getId(), type, now);
        authTokenRepository.flush();

        String rawToken = generateRawToken();
        OffsetDateTime expiresAt = now.plus(ttlFor(type));

        AuthToken authToken = new AuthToken();
        authToken.setUser(user);
        authToken.setTokenHash(hashToken(rawToken));
        authToken.setType(type);
        authToken.setExpiresAt(expiresAt);
        authToken.setCreatedAt(now);
        authTokenRepository.saveAndFlush(authToken);

        return new IssuedToken(rawToken, expiresAt);
    }

    public AuthToken requireActiveToken(String rawToken, AuthTokenType type) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        AuthToken authToken = authTokenRepository.findByTokenHashAndType(hashToken(rawToken), type)
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));

        if (authToken.getRevokedAt() != null || authToken.getUsedAt() != null) {
            if (type == AuthTokenType.REFRESH) {
                authTokenRepository.revokeActiveTokens(authToken.getUser().getId(), AuthTokenType.REFRESH, now);
            }
            throw new IllegalArgumentException("Invalid or expired token");
        }

        if (!authToken.getExpiresAt().isAfter(now)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }

        return authToken;
    }

    public void markUsed(AuthToken authToken) {
        authToken.setUsedAt(OffsetDateTime.now(ZoneOffset.UTC));
        authTokenRepository.save(authToken);
    }

    public void revokeRefreshTokens(UUID userId) {
        authTokenRepository.revokeActiveTokens(userId, AuthTokenType.REFRESH, OffsetDateTime.now(ZoneOffset.UTC));
    }

    public void revokeRefreshToken(String rawToken) {
        authTokenRepository.findByTokenHashAndType(hashToken(rawToken), AuthTokenType.REFRESH)
            .ifPresent(token -> {
                if (token.getRevokedAt() == null && token.getUsedAt() == null) {
                    token.setRevokedAt(OffsetDateTime.now(ZoneOffset.UTC));
                    authTokenRepository.save(token);
                }
            });
    }

    /** Invalidates access JWTs (token version) and refresh tokens for the user. */
    public void revokeSessions(UserAccount user) {
        user.bumpTokenVersion();
        userAccountRepository.saveAndFlush(user);
        revokeRefreshTokens(user.getId());
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Duration ttlFor(AuthTokenType type) {
        return switch (type) {
            case EMAIL_VERIFY -> authProperties.getEmailVerifyTokenTtl();
            case PASSWORD_RESET -> authProperties.getPasswordResetTokenTtl();
            case REFRESH -> authProperties.getRefreshTokenTtl();
        };
    }

    public record IssuedToken(String rawToken, OffsetDateTime expiresAt) {
    }
}
