package com.rufus100procent.monitor.dto;

import java.time.Instant;
import java.util.UUID;

public class RegisteredServerDto {

    private UUID id;

    private String appName;
    private String ServerActuatorUrl; // base + actuator path
    private int pollIntervalSeconds;

    private boolean pause;
    private Instant registeredAt;
    private String status;
    private Instant lastPolledAt;
    private Instant lastSeenUp;
    private Long memoryMaxBytes;
    private Integer cpuCoreCount;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getServerActuatorUrl() {
        return ServerActuatorUrl;
    }

    public void setServerActuatorUrl(String serverActuatorUrl) {
        ServerActuatorUrl = serverActuatorUrl;
    }

    public int getPollIntervalSeconds() {
        return pollIntervalSeconds;
    }

    public void setPollIntervalSeconds(int pollIntervalSeconds) {
        this.pollIntervalSeconds = pollIntervalSeconds;
    }

    public boolean isPause() {
        return pause;
    }

    public Long getMemoryMaxBytes() {
        return memoryMaxBytes;
    }

    public void setMemoryMaxBytes(Long memoryMaxBytes) {
        this.memoryMaxBytes = memoryMaxBytes;
    }

    public Integer getCpuCoreCount() {
        return cpuCoreCount;
    }

    public void setCpuCoreCount(Integer cpuCoreCount) {
        this.cpuCoreCount = cpuCoreCount;
    }

    public void setPause(boolean pause) {
        this.pause = pause;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getLastPolledAt() {
        return lastPolledAt;
    }

    public void setLastPolledAt(Instant lastPolledAt) {
        this.lastPolledAt = lastPolledAt;
    }

    public Instant getLastSeenUp() {
        return lastSeenUp;
    }

    public void setLastSeenUp(Instant lastSeenUp) {
        this.lastSeenUp = lastSeenUp;
    }
}
