package com.rufus100procent.monitor.modal;


import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("server_snapshots")
public class ServerSnapshot {

    @Id
    private UUID id;
    private UUID serverId;

    private Instant polledAt;
    private String serverTimezone;
    private String serverLocalTime;

    private String healthStatus;
    private String appName;
    private String appVersion;

    private Long memoryUsedBytes;
    private Long memoryMaxBytes;
    private Double cpuUsage;
    private Double uptimeSeconds;

    private Long httpRequestCount;
    private Double httpAvgMs;
    private Double httpMaxMs;            // MAX slowest request
    private Long http2xxCount;
    private Long http4xxCount;
    private Long http5xxCount;

    private boolean pollSuccess;
    private String pollErrorMessage;
}