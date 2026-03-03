## Monitor

> Early stage work in progress

Visual Monitoring App tailored for Spring Boot Actuator.

Register any Spring Boot application that exposes actuator endpoints and visualize health,
memory, CPU, uptime and HTTP metrics in one place

## Goal 

- Register unlimited Spring Boot servers by actuator URL
- Polls each server on a configurable interval (7 to 60 seconds)
- Stores historical snapshots in PostgreSQL
- Displays live and historical charts per server
- Multi-user login

## Requirements

- Java 25
- PostgreSQL
- The monitored application must expose Spring Boot Actuator

```properties
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always
```
