package com.inventory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.platform")
public class PlatformProperties {

    private boolean openRegistration = true;
    private String adminEmail = "";
    private String adminPassword = "";
    private boolean adminReset = false;

    public boolean isOpenRegistration() {
        return openRegistration;
    }

    public void setOpenRegistration(boolean openRegistration) {
        this.openRegistration = openRegistration;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public boolean isAdminReset() {
        return adminReset;
    }

    public void setAdminReset(boolean adminReset) {
        this.adminReset = adminReset;
    }
}
