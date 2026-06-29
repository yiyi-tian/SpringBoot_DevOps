USE devops_message;

CREATE TABLE IF NOT EXISTS msg_carrier (
    carrier_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(128) NOT NULL,
    provider     VARCHAR(64)  NOT NULL DEFAULT '',
    channel_type VARCHAR(32)  NOT NULL COMMENT 'IN_APP/TENCENT_SMS/EMAIL',
    config_json  TEXT NULL COMMENT 'SMTP or SMS config JSON',
    enabled      TINYINT(1)   NOT NULL DEFAULT 1,
    deleted_at   DATETIME NULL,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_channel_enabled (channel_type, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS msg_template (
    template_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(128) NOT NULL,
    content      TEXT NOT NULL,
    channel_type VARCHAR(32) NOT NULL,
    status       VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/ACTIVE/DISABLED',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_channel_status (channel_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS msg_message (
    message_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id      BIGINT NULL,
    carrier_id       BIGINT NULL,
    initiator_user_id BIGINT NULL,
    receiver         VARCHAR(255) NOT NULL,
    channel_type     VARCHAR(32) NOT NULL,
    rendered_content TEXT NULL,
    status           VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
    provider_msg_id  VARCHAR(128) NULL,
    send_time        DATETIME DEFAULT CURRENT_TIMESTAMP,
    error_message    VARCHAR(512) NULL,
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_receiver (receiver),
    KEY idx_channel_time (channel_type, send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 欢迎站内信（注册编排 templateId=1）
INSERT INTO msg_template (template_id, name, content, channel_type, status)
SELECT 1, 'welcome-in-app', '欢迎加入 DevOps 平台！', 'IN_APP', 'ACTIVE'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM msg_template WHERE template_id = 1);
