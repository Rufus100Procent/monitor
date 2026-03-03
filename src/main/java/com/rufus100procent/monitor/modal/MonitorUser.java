package com.rufus100procent.monitor.modal;


import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("monitor_users")
public class MonitorUser {

    @Id
    private UUID id;
    private String username;
    private String passwordHash;
    private String role;
    private Instant createdAt;
    private Instant lastLoginAt;
}