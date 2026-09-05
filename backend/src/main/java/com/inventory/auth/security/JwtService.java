package com.inventory.auth.security;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.inventory.config.AuthProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final AuthProperties authProperties;
    private final SecretKey secretKey;

    public JwtService(AuthProperties authProperties) {
        this.authProperties = authProperties;
        String secret = authProperties.getJwtSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("APP_JWT_SECRET / app.auth.jwt-secret must be set");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public JwtToken issueToken(UUID userId, String email) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime expiresAt = now.plus(authProperties.getAccessTokenTtl());

        String token = Jwts.builder()
            .subject(userId.toString())
            .claim("email", email == null ? "" : email)
            .issuedAt(Date.from(now.toInstant()))
            .expiration(Date.from(expiresAt.toInstant()))
            .signWith(secretKey)
            .compact();

        return new JwtToken(token, expiresAt);
    }

    public AuthenticatedUser parse(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            return new AuthenticatedUser(
                UUID.fromString(claims.getSubject()),
                claims.get("email", String.class),
                false
            );
        } catch (JwtException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid or expired access token");
        }
    }

    public record JwtToken(String value, OffsetDateTime expiresAt) {
    }
}
