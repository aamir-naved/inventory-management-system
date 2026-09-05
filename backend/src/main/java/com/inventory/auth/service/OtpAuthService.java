package com.inventory.auth.service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.auth.dto.AuthResponse;
import com.inventory.auth.dto.MessageResponse;
import com.inventory.auth.entity.PhoneOtp;
import com.inventory.auth.entity.UserAccount;
import com.inventory.auth.repository.PhoneOtpRepository;
import com.inventory.auth.repository.UserAccountRepository;
import com.inventory.auth.sms.SmsGateway;
import com.inventory.auth.support.PhoneNumbers;
import com.inventory.config.AuthProperties;
import com.inventory.config.RegistrationPolicy;

@Service
@Transactional
public class OtpAuthService {

    private static final int MAX_ATTEMPTS = 5;

    private final PhoneOtpRepository phoneOtpRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final SmsGateway smsGateway;
    private final AuthProperties authProperties;
    private final AuthService authService;
    private final RegistrationPolicy registrationPolicy;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpAuthService(
        PhoneOtpRepository phoneOtpRepository,
        UserAccountRepository userAccountRepository,
        PasswordEncoder passwordEncoder,
        SmsGateway smsGateway,
        AuthProperties authProperties,
        AuthService authService,
        RegistrationPolicy registrationPolicy
    ) {
        this.phoneOtpRepository = phoneOtpRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.smsGateway = smsGateway;
        this.authProperties = authProperties;
        this.authService = authService;
        this.registrationPolicy = registrationPolicy;
    }

    public MessageResponse requestCode(String rawPhone) {
        String phone = PhoneNumbers.normalize(rawPhone);
        UserAccount existing = userAccountRepository.findByPhone(phone).orElse(null);
        if (existing == null && !registrationPolicy.isOpen()) {
            throw new IllegalArgumentException("This number is not registered. Ask the operator to create your shop.");
        }
        if (existing != null && !existing.isActive()) {
            throw new IllegalArgumentException("This account is disabled");
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        phoneOtpRepository.findFirstByPhoneOrderByCreatedAtDesc(phone).ifPresent(latest -> {
            if (latest.getConsumedAt() == null
                && latest.getCreatedAt().plusSeconds(authProperties.getPhoneOtpResendSeconds()).isAfter(now)) {
                throw new IllegalArgumentException("Wait a moment before requesting another code");
            }
        });

        int code = 100000 + secureRandom.nextInt(900000);
        String codeText = String.valueOf(code);

        PhoneOtp otp = new PhoneOtp();
        otp.setPhone(phone);
        otp.setCodeHash(AuthTokenService.hashToken(codeText));
        otp.setExpiresAt(now.plus(authProperties.getPhoneOtpTtl()));
        otp.setCreatedAt(now);
        otp.setAttempts(0);
        phoneOtpRepository.save(otp);

        smsGateway.send(phone, "Your shop login code is " + codeText + ". It expires in 5 minutes.");
        return new MessageResponse("OTP sent to " + phone);
    }

    public AuthResponse verify(String rawPhone, String code) {
        String phone = PhoneNumbers.normalize(rawPhone);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        PhoneOtp otp = phoneOtpRepository.findFirstByPhoneOrderByCreatedAtDesc(phone)
            .orElseThrow(() -> new IllegalArgumentException("Request a code first"));

        if (otp.getConsumedAt() != null || !otp.getExpiresAt().isAfter(now)) {
            throw new IllegalArgumentException("This code has expired. Request a new one.");
        }

        otp.setAttempts(otp.getAttempts() + 1);
        if (!AuthTokenService.hashToken(code.trim()).equals(otp.getCodeHash())) {
            if (otp.getAttempts() >= MAX_ATTEMPTS) {
                otp.setConsumedAt(now);
            }
            phoneOtpRepository.save(otp);
            throw new IllegalArgumentException("Wrong code. Try again.");
        }

        otp.setConsumedAt(now);
        phoneOtpRepository.save(otp);

        UserAccount user = userAccountRepository.findByPhone(phone).orElse(null);
        if (user == null) {
            if (!registrationPolicy.isOpen()) {
                throw new IllegalArgumentException("This number is not registered. Ask the operator to create your shop.");
            }
            user = createPhoneUser(phone);
        }
        if (!user.isActive()) {
            throw new IllegalArgumentException("This account is disabled");
        }
        return authService.issueSession(user, true);
    }

    private UserAccount createPhoneUser(String phone) {
        UserAccount user = new UserAccount();
        user.setFullName("Shop owner");
        user.setPhone(phone);
        user.setEmail(null);
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setEmailVerified(true);
        user.setActive(true);
        return userAccountRepository.saveAndFlush(user);
    }
}
