package com.inventory.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.inventory.auth.entity.PlatformRole;
import com.inventory.auth.entity.UserAccount;
import com.inventory.auth.repository.UserAccountRepository;

@Component
public class PlatformAdminBootstrap {

    private static final Logger log = LoggerFactory.getLogger(PlatformAdminBootstrap.class);

    private final PlatformProperties platformProperties;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public PlatformAdminBootstrap(
        PlatformProperties platformProperties,
        UserAccountRepository userAccountRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.platformProperties = platformProperties;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensurePlatformAdmin() {
        String email = trimToNull(platformProperties.getAdminEmail());
        if (email == null) {
            return;
        }
        email = email.toLowerCase();
        String password = platformProperties.getAdminPassword();
        UserAccount existing = userAccountRepository.findByEmailIgnoreCase(email).orElse(null);
        if (existing != null) {
            if (platformProperties.isAdminReset()) {
                if (!StringUtils.hasText(password)) {
                    log.warn("APP_PLATFORM_ADMIN_RESET is true but APP_PLATFORM_ADMIN_PASSWORD is empty; skip");
                    return;
                }
                existing.setPasswordHash(passwordEncoder.encode(password));
                existing.setPlatformRole(PlatformRole.PLATFORM_ADMIN);
                existing.setActive(true);
                existing.setEmailVerified(true);
                userAccountRepository.save(existing);
                log.info("Reset platform admin password for {}", email);
            }
            return;
        }
        if (!StringUtils.hasText(password)) {
            log.warn("APP_PLATFORM_ADMIN_EMAIL is set but APP_PLATFORM_ADMIN_PASSWORD is empty; skip bootstrap");
            return;
        }
        UserAccount admin = new UserAccount();
        admin.setFullName("Platform admin");
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setEmailVerified(true);
        admin.setActive(true);
        admin.setPlatformRole(PlatformRole.PLATFORM_ADMIN);
        userAccountRepository.save(admin);
        log.info("Created platform admin user {}", email);
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
