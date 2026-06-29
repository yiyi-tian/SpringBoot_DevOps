-- ==========================================
-- devops_message 库
-- ==========================================

USE devops_message;

CREATE TABLE IF NOT EXISTS `msg_message` (
    `message_id`       BIGINT AUTO_INCREMENT PRIMARY KEY,
    `task_id`          BIGINT NULL COMMENT '关联任务ID',
    `template_id`      BIGINT NULL COMMENT '模板ID',
    `carrier_id`       BIGINT NULL COMMENT '载体ID',
    `receiver`         VARCHAR(255) NOT NULL COMMENT '接收人（userId/手机号/邮箱）',
    `rendered_content` TEXT NULL COMMENT '消息内容',
    `status`           VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED',
    `provider_msg_id`  VARCHAR(128) NULL COMMENT '第三方回执ID',
    `send_time`        DATETIME NULL COMMENT '发送时间',
    `error_message`    VARCHAR(512) NULL COMMENT '错误信息',
    `created_at`       DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_receiver` (`receiver`, `created_at`),
    KEY `idx_status`   (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 消息载体配置（后续短信/邮件需要）
CREATE TABLE IF NOT EXISTS `msg_carrier` (
    `carrier_id`   BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name`         VARCHAR(128) NOT NULL COMMENT '载体名称',
    `provider`     VARCHAR(64) NOT NULL COMMENT '供应商',
    `channel_type` VARCHAR(32) NOT NULL COMMENT 'IN_APP/TENCENT_SMS/EMAIL',
    `config_json`  TEXT NOT NULL COMMENT '配置JSON（密钥等）',
    `enabled`      TINYINT(1) DEFAULT 1,
    `deleted_at`   DATETIME NULL COMMENT '软删除',
    `created_at`   DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 消息模板
CREATE TABLE IF NOT EXISTS `msg_template` (
    `template_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name`        VARCHAR(128) NOT NULL COMMENT '模板名称',
    `content`     TEXT NOT NULL COMMENT '模板正文',
    `channel_type` VARCHAR(32) NOT NULL COMMENT '渠道类型',
    `status`      VARCHAR(32) DEFAULT 'DRAFT' COMMENT 'DRAFT/ACTIVE/DISABLED',
    `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;