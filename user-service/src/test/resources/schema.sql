-- H2 测试数据库 Schema
-- MODE=MySQL 启用反引号支持; NON_KEYWORDS=GROUP,USER 避免保留字冲突

CREATE TABLE IF NOT EXISTS `user` (
    user_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    display_name   VARCHAR(255),
    sex            INT,
    status         VARCHAR(20)  DEFAULT 'ACTIVE',
    is_deleted     SMALLINT     DEFAULT 0,
    last_login_at  TIMESTAMP    NULL,
    last_login_ip  VARCHAR(45)  NULL,
    created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_auth (
    auth_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    identity_type VARCHAR(20)  NOT NULL,
    identifier    VARCHAR(255) NOT NULL,
    secret_hash   VARCHAR(255),
    verified      SMALLINT     DEFAULT 1,
    expired_at    TIMESTAMP    NULL,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `group` (
    group_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    creator_user_id BIGINT,
    is_admin        SMALLINT     DEFAULT 0,
    is_deleted      SMALLINT     DEFAULT 0,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS permission (
    perm_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    perm_code VARCHAR(100) NOT NULL,
    perm_name VARCHAR(200) NOT NULL,
    active    SMALLINT     DEFAULT 1
);

CREATE TABLE IF NOT EXISTS user_group (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id  BIGINT NOT NULL,
    group_id BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS user_permission (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    perm_id    BIGINT      NOT NULL,
    status     VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS group_permission (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id   BIGINT      NOT NULL,
    perm_id    BIGINT      NOT NULL,
    status     VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS attribute (
    attr_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    attr_key   VARCHAR(100) NOT NULL,
    attr_value VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS login_history (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    client_ip  VARCHAR(45),
    user_agent VARCHAR(500),
    session_id VARCHAR(255),
    login_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_session (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    device_id       VARCHAR(64)  NOT NULL,
    session_id      VARCHAR(255) NOT NULL,
    device_type     VARCHAR(20)  DEFAULT 'UNKNOWN',
    client_ip       VARCHAR(45),
    user_agent      VARCHAR(500),
    login_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    last_active_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    status          VARCHAR(20)  DEFAULT 'ACTIVE'
);

-- RBAC seed (aligned with docs/sql/02b_user_rbac_seed.sql)
DELETE FROM group_permission WHERE group_id IN (1, 2);
DELETE FROM `group` WHERE group_id IN (1, 2);
DELETE FROM permission WHERE perm_id IN (1, 2, 3, 4, 5, 6, 7);

INSERT INTO permission (perm_id, perm_code, perm_name, active) VALUES
    (1, 'profile:read', 'profile read', 1),
    (2, 'profile:write', 'profile write', 1),
    (3, 'message:read', 'message read', 1),
    (4, 'permissions:apply', 'permissions apply', 1),
    (5, 'log:read', 'log read', 1),
    (6, 'log:export', 'log export', 1),
    (7, 'message:write', 'message write', 1);

INSERT INTO `group` (group_id, name, description, is_admin, is_deleted) VALUES
    (1, 'member', 'default member group', 0, 0),
    (2, 'admin', 'admin group', 1, 0);

INSERT INTO group_permission (group_id, perm_id, status) VALUES
    (1, 1, 'ACTIVE'), (1, 2, 'ACTIVE'), (1, 3, 'ACTIVE'), (1, 4, 'ACTIVE'),
    (2, 1, 'ACTIVE'), (2, 2, 'ACTIVE'), (2, 3, 'ACTIVE'), (2, 4, 'ACTIVE');
