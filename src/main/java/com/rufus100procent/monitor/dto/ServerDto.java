package com.rufus100procent.monitor.dto;

public class ServerDto {
    private String name;
    private String baseUrl;
    private String actuatorPath;
    private int pollIntervalSeconds;

    public void setName(String name) {
        this.name = name;
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

    public String getName() {
        return name;
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