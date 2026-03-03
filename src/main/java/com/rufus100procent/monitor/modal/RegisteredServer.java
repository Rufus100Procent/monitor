package com.rufus100procent.monitor.modal;


import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("registered_servers")
public class RegisteredServer {

    @Id
    private UUID id;
    private String name;
    private String baseUrl;
    private String actuatorPath;
    private String secret;
    private int pollIntervalSeconds;
    private String status;
    private boolean active;
    private Instant registeredAt;
    private Instant lastPolledAt;
    private Instant lastSeenUp;
}