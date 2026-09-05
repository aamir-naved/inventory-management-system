package com.inventory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.desktop")
public class DesktopProperties {

    private boolean enabled = false;
    private String dataDir = "";
    private String pgBin = "";
    private String dbHost = "127.0.0.1";
    private int dbPort = 5432;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    public String getPgBin() {
        return pgBin;
    }

    public void setPgBin(String pgBin) {
        this.pgBin = pgBin;
    }

    public String getDbHost() {
        return dbHost;
    }

    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    public int getDbPort() {
        return dbPort;
    }

    public void setDbPort(int dbPort) {
        this.dbPort = dbPort;
    }
}
