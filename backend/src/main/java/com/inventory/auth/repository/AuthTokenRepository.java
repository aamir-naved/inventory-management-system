package com.inventory.auth.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.inventory.auth.entity.AuthToken;
import com.inventory.auth.entity.AuthTokenType;

public interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {

    Optional<AuthToken> findByTokenHashAndType(String tokenHash, AuthTokenType type);

    @Modifying(clearAutomatically = true)
    @Query("""
        update AuthToken token
        set token.revokedAt = :revokedAt
        where token.user.id = :userId
          and token.type = :type
          and token.revokedAt is null
          and token.usedAt is null
        """)
    int revokeActiveTokens(
        @Param("userId") UUID userId,
        @Param("type") AuthTokenType type,
        @Param("revokedAt") OffsetDateTime revokedAt
    );
}
