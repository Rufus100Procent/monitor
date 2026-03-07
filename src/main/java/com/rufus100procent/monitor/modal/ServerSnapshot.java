package com.rufus100procent.monitor.modal;


import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("server_snapshots")
public class ServerSnapshot implements Persistable<UUID>  {

    @Id
    private UUID id;
    private UUID serverId;

    @Transient
    private boolean isNew = true;

    private Instant polledAt;

    private String healthStatus;
    private String appVersion;

    private Long memoryUsedBytes;
    private Double systemLoad;
    private Double cpuUsage;

    private Double uptimeSeconds;

    private Long httpRequestCount;
    private Double httpAvgMs;

    @Column("http_2xx_count")
    private Long http2xxCount;

    @Column("http_4xx_count")
    private Long http4xxCount;

    @Column("http_5xx_count")
    private Long http5xxCount;

    private Long jvmThreadsLive;
    private Double gcOverhead;

    private Long diskTotalBytes;
    private Long diskFreeBytes;

    private boolean pollSuccess;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public @Nullable UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getServerId() {
        return serverId;
    }

    public void setServerId(UUID serverId) {
        this.serverId = serverId;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public Instant getPolledAt() {
        return polledAt;
    }

    public Double getSystemLoad() {
        return systemLoad;
    }

    public void setSystemLoad(Double systemLoad) {
        this.systemLoad = systemLoad;
    }

    public Long getJvmThreadsLive() {
        return jvmThreadsLive;
    }

    public void setJvmThreadsLive(Long jvmThreadsLive) {
        this.jvmThreadsLive = jvmThreadsLive;
    }

    public Double getGcOverhead() {
        return gcOverhead;
    }

    public void setGcOverhead(Double gcOverhead) {
        this.gcOverhead = gcOverhead;
    }

    public void setPolledAt(Instant polledAt) {
        this.polledAt = polledAt;
    }

    public Long getDiskTotalBytes() {
        return diskTotalBytes;
    }

    public void setDiskTotalBytes(Long diskTotalBytes) {
        this.diskTotalBytes = diskTotalBytes;
    }

    public Long getDiskFreeBytes() {
        return diskFreeBytes;
    }

    public void setDiskFreeBytes(Long diskFreeBytes) {
        this.diskFreeBytes = diskFreeBytes;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public Long getMemoryUsedBytes() {
        return memoryUsedBytes;
    }

    public void setMemoryUsedBytes(Long memoryUsedBytes) {
        this.memoryUsedBytes = memoryUsedBytes;
    }

    public Double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(Double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public Double getUptimeSeconds() {
        return uptimeSeconds;
    }

    public void setUptimeSeconds(Double uptimeSeconds) {
        this.uptimeSeconds = uptimeSeconds;
    }

    public Long getHttpRequestCount() {
        return httpRequestCount;
    }

    public void setHttpRequestCount(Long httpRequestCount) {
        this.httpRequestCount = httpRequestCount;
    }

    public Double getHttpAvgMs() {
        return httpAvgMs;
    }

    public void setHttpAvgMs(Double httpAvgMs) {
        this.httpAvgMs = httpAvgMs;
    }

    public Long getHttp2xxCount() {
        return http2xxCount;
    }

    public void setHttp2xxCount(Long http2xxCount) {
        this.http2xxCount = http2xxCount;
    }

    public Long getHttp4xxCount() {
        return http4xxCount;
    }

    public void setHttp4xxCount(Long http4xxCount) {
        this.http4xxCount = http4xxCount;
    }

    public Long getHttp5xxCount() {
        return http5xxCount;
    }

    public void setHttp5xxCount(Long http5xxCount) {
        this.http5xxCount = http5xxCount;
    }

    public boolean isPollSuccess() {
        return pollSuccess;
    }

    public void setPollSuccess(boolean pollSuccess) {
        this.pollSuccess = pollSuccess;
    }

}