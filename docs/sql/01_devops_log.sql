USE devops_log;

CREATE TABLE IF NOT EXISTS audit_log (
    log_id      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    trace_id    VARCHAR(64)  NULL,
    user_id     BIGINT       NULL,
    operation   VARCHAR(64)  NOT NULL,
    success     TINYINT(1)   NOT NULL DEFAULT 1,
    target_id   VARCHAR(64)  NULL,
    detail      TEXT         NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_user_id (user_id),
    INDEX idx_audit_operation (operation),
    INDEX idx_audit_created_at (created_at),
    INDEX idx_audit_trace_id (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS metrics_threshold_config (
    config_key       VARCHAR(64)  NOT NULL PRIMARY KEY,
    threshold_value  DOUBLE       NOT NULL,
    severity         VARCHAR(16)  NOT NULL DEFAULT 'NORMAL',
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO metrics_threshold_config (config_key, threshold_value, severity) VALUES
    ('error_rate_max', 0.05, 'WARN'),
    ('p99_max', 3000, 'WARN'),
    ('success_rate_min', 0.95, 'WARN'),
    ('slow_count_max', 100, 'NORMAL')
ON DUPLICATE KEY UPDATE threshold_value = VALUES(threshold_value);
