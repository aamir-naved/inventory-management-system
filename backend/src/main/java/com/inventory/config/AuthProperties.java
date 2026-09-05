package com.inventory.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private String jwtSecret;
    private Duration accessTokenTtl = Duration.ofHours(1);
    private Duration refreshTokenTtl = Duration.ofDays(14);
    private Duration emailVerifyTokenTtl = Duration.ofHours(24);
    private Duration passwordResetTokenTtl = Duration.ofHours(1);
    private String publicAppUrl = "http://localhost:5173";
    private String mailFrom = "noreply@inventory.local";
    private Duration phoneOtpTtl = Duration.ofMinutes(5);
    private int phoneOtpResendSeconds = 45;
    private String smsWebhookUrl;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public Duration getEmailVerifyTokenTtl() {
        return emailVerifyTokenTtl;
    }

    public void setEmailVerifyTokenTtl(Duration emailVerifyTokenTtl) {
        this.emailVerifyTokenTtl = emailVerifyTokenTtl;
    }

    public Duration getPasswordResetTokenTtl() {
        return passwordResetTokenTtl;
    }

    public void setPasswordResetTokenTtl(Duration passwordResetTokenTtl) {
        this.passwordResetTokenTtl = passwordResetTokenTtl;
    }

    public String getPublicAppUrl() {
        return publicAppUrl;
    }

    public void setPublicAppUrl(String publicAppUrl) {
        this.publicAppUrl = publicAppUrl;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public Duration getPhoneOtpTtl() {
        return phoneOtpTtl;
    }

    public void setPhoneOtpTtl(Duration phoneOtpTtl) {
        this.phoneOtpTtl = phoneOtpTtl;
    }

    public int getPhoneOtpResendSeconds() {
        return phoneOtpResendSeconds;
    }

    public void setPhoneOtpResendSeconds(int phoneOtpResendSeconds) {
        this.phoneOtpResendSeconds = phoneOtpResendSeconds;
    }

    public String getSmsWebhookUrl() {
        return smsWebhookUrl;
    }

    public void setSmsWebhookUrl(String smsWebhookUrl) {
        this.smsWebhookUrl = smsWebhookUrl;
    }
}
