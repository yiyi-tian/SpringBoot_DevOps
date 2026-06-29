-- =====================================================
-- user-service 数据库建表脚本
-- 数据库：MySQL
-- 用法：mysql -u root -p < docs/sql/02_devops_user.sql
-- 已有库迁移：02a_user_rbac_migrate.sql → 02b_user_rbac_seed.sql
-- =====================================================


USE devops_user;

-- 1. 用户主表（最先建，被其他表外键引用）
CREATE TABLE IF NOT EXISTS `user` (
    `user_id`       BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户唯一标识',
    `display_name`  VARCHAR(128) NOT NULL COMMENT '用户昵称/展示名',
    `sex`           TINYINT      NULL COMMENT '性别（可选）',
    `status`        VARCHAR(32)  NOT NULL DEFAULT 'active' COMMENT '用户状态：active, locked, deregistered',
    `is_deleted`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    `last_login_at` DATETIME     NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(45)  NULL COMMENT '最后登录 IP',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_status` (`status`),
    INDEX `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户主表';

-- 2. 用户组/角色表（依赖 user 表）
CREATE TABLE IF NOT EXISTS `group` (
    `group_id`        BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '组/角色唯一标识',
    `name`            VARCHAR(255) NOT NULL COMMENT '组/角色名称',
    `description`     VARCHAR(255) NULL COMMENT '组/角色描述',
    `creator_user_id` BIGINT       NULL COMMENT '创建者用户ID（可选）',
    `is_admin`        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否为管理员组：0-普通组，1-管理员组',
    `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_creator` (`creator_user_id`),
    INDEX `idx_is_deleted` (`is_deleted`),
    CONSTRAINT `fk_group_creator` FOREIGN KEY (`creator_user_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户组/角色表';

-- 3. 权限表
CREATE TABLE IF NOT EXISTS `permission` (
    `perm_id`   BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '权限唯一标识',
    `perm_code` VARCHAR(255) NOT NULL COMMENT '权限编码，如 user:add',
    `perm_name` VARCHAR(255) NOT NULL COMMENT '权限名称',
    `active`    TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
    UNIQUE KEY `uk_perm_code` (`perm_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 4. 用户认证凭证表（依赖 user）
CREATE TABLE IF NOT EXISTS `user_auth` (
    `auth_id`       BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '凭证唯一标识',
    `user_id`       BIGINT       NOT NULL COMMENT '关联用户ID',
    `identity_type` VARCHAR(32)  NOT NULL COMMENT '凭证类型：PHONE, EMAIL, USERNAME, FEISHU, WECHAT',
    `identifier`    VARCHAR(255) NOT NULL COMMENT '凭证标识（手机号/邮箱/OpenID等）',
    `secret_hash`   VARCHAR(255) NOT NULL COMMENT 'BCrypt 密码哈希',
    `verified`      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否已验证',
    `expired_at`    DATETIME     NULL COMMENT '凭证过期时间，NULL表示永不过期',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_identity` (`identity_type`, `identifier`),
    INDEX `idx_user_id` (`user_id`),
    CONSTRAINT `fk_user_auth_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户认证凭证表';

-- 5. 用户扩展属性表（依赖 user）
CREATE TABLE IF NOT EXISTS `attribute` (
    `attr_id`    BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '属性记录唯一标识',
    `user_id`    BIGINT       NULL COMMENT '关联用户ID（可选）',
    `attr_key`   VARCHAR(64)  NOT NULL COMMENT '属性键名',
    `attr_value` VARCHAR(512) NOT NULL COMMENT '属性值',
    INDEX `idx_user_id` (`user_id`),
    CONSTRAINT `fk_attribute_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户扩展属性表';

-- 6. 用户-组关联表（依赖 user 和 group）
CREATE TABLE IF NOT EXISTS `user_group` (
    `id`       BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关联记录唯一标识',
    `user_id`  BIGINT NOT NULL COMMENT '关联用户ID',
    `group_id` BIGINT NOT NULL COMMENT '关联用户组ID',
    UNIQUE KEY `uk_user_group` (`user_id`, `group_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_group_id` (`group_id`),
    CONSTRAINT `fk_user_group_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_group_group` FOREIGN KEY (`group_id`) REFERENCES `group` (`group_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-组关联表';

-- 7. 组-权限关联表（依赖 group 和 permission）
CREATE TABLE IF NOT EXISTS `group_permission` (
    `id`         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关联记录唯一标识',
    `group_id`   BIGINT NOT NULL COMMENT '关联用户组ID',
    `perm_id`    BIGINT NOT NULL COMMENT '关联权限ID',
    `status`     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/PENDING/REJECTED',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_group_permission` (`group_id`, `perm_id`),
    INDEX `idx_group_id` (`group_id`),
    INDEX `idx_perm_id` (`perm_id`),
    CONSTRAINT `fk_group_permission_group` FOREIGN KEY (`group_id`) REFERENCES `group` (`group_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_group_permission_perm` FOREIGN KEY (`perm_id`) REFERENCES `permission` (`perm_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组-权限关联表';

-- 8. 用户-权限关联表（依赖 user 和 permission）
CREATE TABLE IF NOT EXISTS `user_permission` (
    `id`         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关联记录唯一标识',
    `user_id`    BIGINT NOT NULL COMMENT '关联用户ID',
    `perm_id`    BIGINT NOT NULL COMMENT '关联权限ID',
    `status`     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/PENDING/REJECTED',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_user_permission` (`user_id`, `perm_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_perm_id` (`perm_id`),
    CONSTRAINT `fk_user_permission_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_permission_perm` FOREIGN KEY (`perm_id`) REFERENCES `permission` (`perm_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-权限关联表';

-- 9. 登录历史
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

-- 10. 多端会话
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

-- 种子数据见 02b_user_rbac_seed.sql
