package com.rufus100procent.monitor.dto;

public class ServerDto {
    private String appName;
    private String appVersion;
    private String baseUrl;
    private String actuatorPath;
    private int pollIntervalSeconds;

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setActuatorPath(String actuatorPath) {
        this.actuatorPath = actuatorPath;
    }

    public void setPollIntervalSeconds(int pollIntervalSeconds) {
        this.pollIntervalSeconds = pollIntervalSeconds;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getAppName() {
        return appName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getActuatorPath() {
        return actuatorPath;
    }

    public int getPollIntervalSeconds() {
        return pollIntervalSeconds;
    }
}