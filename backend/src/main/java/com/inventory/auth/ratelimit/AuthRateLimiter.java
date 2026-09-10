package com.inventory.auth.ratelimit;

import com.inventory.common.api.RateLimitExceededException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window limiter for auth endpoints.
 * Suitable for a single-node deploy; replace with a shared store if the API is horizontally scaled.
 */
@Component
public class AuthRateLimiter {

    public static final String ACTION_LOGIN = "login";
    public static final String ACTION_OTP_REQUEST = "otp-request";
    public static final String ACTION_OTP_VERIFY = "otp-verify";
    public static final String ACTION_FORGOT_PASSWORD = "forgot-password";

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final ConcurrentHashMap<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public void checkLogin(String clientIp, String email) {
        check(ACTION_LOGIN + ":ip:" + clientIp, 10, "Too many login attempts from this network. Try again in a minute.");
        check(ACTION_LOGIN + ":acct:" + normalize(email), 5, "Too many login attempts for this account. Try again in a minute.");
    }

    public void checkOtpRequest(String clientIp, String phone) {
        check(ACTION_OTP_REQUEST + ":ip:" + clientIp, 5, "Too many OTP requests from this network. Try again in a minute.");
        check(ACTION_OTP_REQUEST + ":acct:" + normalize(phone), 3, "Too many OTP requests for this phone. Try again in a minute.");
    }

    public void checkOtpVerify(String clientIp, String phone) {
        check(ACTION_OTP_VERIFY + ":ip:" + clientIp, 20, "Too many OTP verification attempts from this network. Try again in a minute.");
        check(ACTION_OTP_VERIFY + ":acct:" + normalize(phone), 10, "Too many OTP verification attempts for this phone. Try again in a minute.");
    }

    public void checkForgotPassword(String clientIp, String email) {
        check(ACTION_FORGOT_PASSWORD + ":ip:" + clientIp, 5, "Too many password reset requests from this network. Try again in a minute.");
        check(ACTION_FORGOT_PASSWORD + ":acct:" + normalize(email), 3, "Too many password reset requests for this account. Try again in a minute.");
    }

    public void clear() {
        hits.clear();
    }

    private void check(String key, int maxAttempts, String message) {
        long now = System.currentTimeMillis();
        long cutoff = now - WINDOW.toMillis();
        Deque<Long> queue = hits.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (queue) {
            while (!queue.isEmpty() && queue.peekFirst() < cutoff) {
                queue.removeFirst();
            }
            if (queue.size() >= maxAttempts) {
                throw new RateLimitExceededException(message);
            }
            queue.addLast(now);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
