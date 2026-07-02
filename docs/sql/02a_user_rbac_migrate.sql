-- =====================================================
-- user-service RBAC / 登录会话 结构迁移（已有库可重复执行）
-- 用法：mysql -u root -p < docs/sql/02a_user_rbac_migrate.sql
-- 前置：已执行 02_devops_user.sql
-- 后续：02b_user_rbac_seed.sql
-- =====================================================

USE devops_user;

-- user 表：登录字段
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'last_login_at'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `user` ADD COLUMN `last_login_at` DATETIME NULL COMMENT ''最后登录时间'' AFTER `is_deleted`',
    'SELECT ''skip last_login_at''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'last_login_ip'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `user` ADD COLUMN `last_login_ip` VARCHAR(45) NULL COMMENT ''最后登录 IP'' AFTER `last_login_at`',
    'SELECT ''skip last_login_ip''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- user_permission：status / created_at
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_permission' AND COLUMN_NAME = 'status'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `user_permission` ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT ''ACTIVE'' COMMENT ''ACTIVE/PENDING/REJECTED'' AFTER `perm_id`',
    'SELECT ''skip user_permission.status''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_permission' AND COLUMN_NAME = 'created_at'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `user_permission` ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `status`',
    'SELECT ''skip user_permission.created_at''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- group_permission：status / created_at
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'group_permission' AND COLUMN_NAME = 'status'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `group_permission` ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT ''ACTIVE'' COMMENT ''ACTIVE/PENDING/REJECTED'' AFTER `perm_id`',
    'SELECT ''skip group_permission.status''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'group_permission' AND COLUMN_NAME = 'created_at'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `group_permission` ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `status`',
    'SELECT ''skip group_permission.created_at''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- login_history
CREATE TABLE IF NOT EXISTS `login_history` (
    `id`         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录 ID',
    `user_id`    BIGINT       NOT NULL COMMENT '用户 ID',
    `client_ip`  VARCHAR(45)  NULL COMMENT '客户端 IP',
    `user_agent` VARCHAR(500) NULL COMMENT 'User-Agent',
    `session_id` VARCHAR(255) NULL COMMENT '会话 ID',
    `login_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    INDEX `idx_login_user` (`user_id`),
    INDEX `idx_login_at` (`login_at`),
    CONSTRAINT `fk_login_history_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录历史';

-- user_session
CREATE TABLE IF NOT EXISTS `user_session` (
    `id`             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录 ID',
    `user_id`        BIGINT       NOT NULL COMMENT '用户 ID',
    `device_id`      VARCHAR(64)  NOT NULL COMMENT '客户端设备号（UUID）',
    `session_id`     VARCHAR(255) NOT NULL COMMENT 'Shiro 会话 ID',
    `device_type`    VARCHAR(20)  NOT NULL DEFAULT 'UNKNOWN' COMMENT '设备类型',
    `client_ip`      VARCHAR(45)  NULL COMMENT '客户端 IP',
    `user_agent`     VARCHAR(500) NULL COMMENT 'User-Agent',
    `login_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    `last_active_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    `status`         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/TERMINATED',
    UNIQUE KEY `uk_session_id` (`session_id`),
    UNIQUE KEY `uk_user_device` (`user_id`, `device_id`),
    INDEX `idx_session_user` (`user_id`),
    CONSTRAINT `fk_user_session_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户多端会话';

-- user_session.device_id（多端设备唯一标识）
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_session' AND COLUMN_NAME = 'device_id'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `user_session` ADD COLUMN `device_id` VARCHAR(64) NULL COMMENT ''客户端设备号（UUID）'' AFTER `user_id`',
    'SELECT ''skip user_session.device_id''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `user_session` SET `device_id` = CONCAT('legacy-', `session_id`) WHERE `device_id` IS NULL OR `device_id` = '';

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_session'
    AND COLUMN_NAME = 'device_id' AND IS_NULLABLE = 'YES'
);
SET @sql = IF(@col_exists > 0,
    'ALTER TABLE `user_session` MODIFY COLUMN `device_id` VARCHAR(64) NOT NULL COMMENT ''客户端设备号（UUID）''',
    'SELECT ''skip user_session.device_id NOT NULL''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_session' AND INDEX_NAME = 'uk_user_device'
);
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `user_session` ADD UNIQUE KEY `uk_user_device` (`user_id`, `device_id`)',
    'SELECT ''skip uk_user_device''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
