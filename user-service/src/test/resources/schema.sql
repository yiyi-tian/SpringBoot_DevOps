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
    session_id      VARCHAR(255) NOT NULL,
    device_type     VARCHAR(20)  DEFAULT 'UNKNOWN',
    client_ip       VARCHAR(45),
    user_agent      VARCHAR(500),
    login_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    last_active_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    status          VARCHAR(20)  DEFAULT 'ACTIVE'
);
