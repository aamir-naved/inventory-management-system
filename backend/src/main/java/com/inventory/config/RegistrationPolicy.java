package com.inventory.config;

import org.springframework.stereotype.Component;

import com.inventory.common.api.ForbiddenException;

@Component
public class RegistrationPolicy {

    private final PlatformProperties platformProperties;

    public RegistrationPolicy(PlatformProperties platformProperties) {
        this.platformProperties = platformProperties;
    }

    public boolean isOpen() {
        return platformProperties.isOpenRegistration();
    }

    public void requireOpenSelfServe(String message) {
        if (!isOpen()) {
            throw new ForbiddenException(message);
        }
    }
}
