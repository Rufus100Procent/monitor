CREATE TABLE IF NOT EXISTS server_register (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    actuator_path VARCHAR(255) NOT NULL,
    secret VARCHAR(500) NOT NULL,
    poll_interval_seconds INT NOT NULL DEFAULT 15,
    status VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN',
    pause BOOLEAN NOT NULL DEFAULT FALSE,
    registered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_polled_at TIMESTAMP WITH TIME ZONE,
    last_seen_up TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_poll_interval CHECK (poll_interval_seconds >= 7 AND poll_interval_seconds <= 60),
    CONSTRAINT uq_base_url_actuator_path UNIQUE (base_url, actuator_path)
);

CREATE TABLE IF NOT EXISTS server_snapshots (
    id UUID PRIMARY KEY,
    server_id UUID NOT NULL,
    polled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    server_timezone VARCHAR(100),
    server_local_time VARCHAR(100),
    health_status VARCHAR(50),
    app_name VARCHAR(255),
    app_version VARCHAR(100),
    memory_used_bytes BIGINT,
    memory_max_bytes BIGINT,
    cpu_usage DOUBLE PRECISION,
    uptime_seconds DOUBLE PRECISION,
    http_request_count BIGINT,
    http_avg_ms DOUBLE PRECISION,
    http_max_ms DOUBLE PRECISION,
    http_2xx_count BIGINT,
    http_4xx_count BIGINT,
    http_5xx_count BIGINT,
    poll_success BOOLEAN NOT NULL DEFAULT TRUE,
    poll_error_message TEXT,
    CONSTRAINT fk_server
        FOREIGN KEY (server_id)
        REFERENCES server_register (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_snapshots_server_id
    ON server_snapshots (server_id);

CREATE INDEX IF NOT EXISTS idx_snapshots_polled_at
    ON server_snapshots (polled_at DESC);

CREATE INDEX IF NOT EXISTS idx_snapshots_server_polled
    ON server_snapshots (server_id, polled_at DESC);