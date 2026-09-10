package com.inventory.auth.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.inventory.common.api.RateLimitExceededException;

class AuthRateLimiterTest {

    private AuthRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new AuthRateLimiter();
    }

    @Test
    void blocksSixthLoginForSameAccountWithinWindow() {
        for (int i = 0; i < 5; i++) {
            limiter.checkLogin("10.0.0." + i, "owner@example.com");
        }

        assertThatThrownBy(() -> limiter.checkLogin("10.0.0.99", "owner@example.com"))
            .isInstanceOf(RateLimitExceededException.class)
            .hasMessageContaining("this account");
    }

    @Test
    void blocksEleventhLoginFromSameIpWithinWindow() {
        for (int i = 0; i < 10; i++) {
            limiter.checkLogin("127.0.0.1", "user" + i + "@example.com");
        }

        assertThatThrownBy(() -> limiter.checkLogin("127.0.0.1", "another@example.com"))
            .isInstanceOf(RateLimitExceededException.class)
            .hasMessageContaining("this network");
    }

    @Test
    void allowsOtpRequestUnderLimit() {
        assertThatCode(() -> {
            limiter.checkOtpRequest("127.0.0.1", "+919876543210");
            limiter.checkOtpRequest("127.0.0.1", "+919876543210");
            limiter.checkOtpRequest("127.0.0.1", "+919876543210");
        }).doesNotThrowAnyException();
    }
}
