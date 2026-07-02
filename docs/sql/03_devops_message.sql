-- ==========================================
-- devops_message 库
-- ==========================================

USE devops_message;

CREATE TABLE IF NOT EXISTS `msg_message` (
    `message_id`       BIGINT AUTO_INCREMENT PRIMARY KEY,
    `task_id`          BIGINT NULL COMMENT '关联任务ID',
    `template_id`      BIGINT NULL COMMENT '模板ID',
    `initiator_user_id` BIGINT NULL,
    `carrier_id`       BIGINT NULL COMMENT '载体ID',
    `receiver`         VARCHAR(255) NOT NULL COMMENT '接收人（userId/手机号/邮箱）',
    `channel_type`     VARCHAR(32) NOT NULL COMMENT '通道类型',
    `rendered_content` TEXT NULL COMMENT '消息内容',
    `status`           VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED',
    `provider_msg_id`  VARCHAR(128) NULL COMMENT '第三方回执ID',
    `send_time`        DATETIME NULL COMMENT '发送时间',
    `error_message`    VARCHAR(512) NULL COMMENT '错误信息',
    `created_at`       DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_receiver (receiver),
    KEY idx_channel_time (channel_type, send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 消息载体配置（后续短信/邮件需要）
CREATE TABLE IF NOT EXISTS `msg_carrier` (
    `carrier_id`   BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name`         VARCHAR(128) NOT NULL COMMENT '载体名称',
    `provider`     VARCHAR(64) NOT NULL DEFAULT '' COMMENT '供应商',
    `channel_type` VARCHAR(32) NOT NULL COMMENT 'IN_APP/TENCENT_SMS/EMAIL',
    `config_json`  TEXT NOT NULL COMMENT '配置JSON（密钥等）',
    `enabled`      TINYINT(1) DEFAULT 1,
    `deleted_at`   DATETIME NULL COMMENT '软删除',
    `created_at`   DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_channel_enabled (channel_type, enabled),
    UNIQUE KEY uk_name_channel (name, channel_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 消息模板
CREATE TABLE IF NOT EXISTS `msg_template` (
    `template_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name`        VARCHAR(128) NOT NULL COMMENT '模板名称',
    `content`     TEXT NOT NULL COMMENT '模板正文',
    `channel_type` VARCHAR(32) NOT NULL COMMENT 'IN_APP/TENCENT_SMS/EMAIL',
    `status`      VARCHAR(32) DEFAULT 'DRAFT' COMMENT 'DRAFT/ACTIVE/DISABLED',
    `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_channel_status (channel_type, status),
    UNIQUE KEY uk_name_channel (name, channel_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 模板变量
CREATE TABLE IF NOT EXISTS `msg_variable` (
    `variable_id`  BIGINT AUTO_INCREMENT PRIMARY KEY,
    `var_key`      VARCHAR(64) NOT NULL COMMENT '变量名（占位符）',
    `name`         VARCHAR(128) NOT NULL COMMENT '变量显示名',
    `type`         VARCHAR(32) NOT NULL DEFAULT 'STRING' COMMENT 'STRING/NUMBER/DATE',
    `required`     TINYINT(1) DEFAULT 1,
    `default_value` VARCHAR(255) NULL,
    `scope`        VARCHAR(32) DEFAULT 'GLOBAL' COMMENT 'GLOBAL/TEMPLATE',
    `status`       VARCHAR(32) DEFAULT 'ACTIVE',
    `description`  VARCHAR(255) NULL,
    `created_at`   DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_var_key` (`var_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 模板变量关联表
CREATE TABLE IF NOT EXISTS `template_variable` (
    `id`                BIGINT AUTO_INCREMENT PRIMARY KEY,
    `template_id`       BIGINT NOT NULL,
    `variable_id`       BIGINT NOT NULL,
    `required_override` TINYINT(1) NULL COMMENT '覆盖默认required',
    `default_override`  VARCHAR(255) NULL COMMENT '覆盖默认default_value',
    `created_at`        DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_template_var` (`template_id`, `variable_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 欢迎站内信（注册编排 templateId=1）
INSERT INTO msg_template (template_id, name, content, channel_type, status)
SELECT 1, 'welcome-in-app', '欢迎加入 DevOps 平台！', 'IN_APP', 'ACTIVE'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM msg_template WHERE template_id = 1);
