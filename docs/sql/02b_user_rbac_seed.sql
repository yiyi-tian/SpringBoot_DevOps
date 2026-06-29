-- =====================================================
-- user-service RBAC 种子数据：基础权限 + member/admin 组
-- 用法：mysql -u root -p < docs/sql/02b_user_rbac_seed.sql
-- 前置：02_devops_user.sql → 02a_user_rbac_migrate.sql
-- =====================================================

USE devops_user;

-- 基础权限（perm_id 1-4 固定，便于关联）
INSERT INTO `permission` (`perm_id`, `perm_code`, `perm_name`, `active`) VALUES
    (1, 'profile:read',       '查看个人资料', 1),
    (2, 'profile:write',      '修改个人资料', 1),
    (3, 'message:read',       '查看消息',     1),
    (4, 'permissions:apply',  '申请权限',     1),
    (5, 'log:read',           '查看运维日志', 1),
    (6, 'log:export',         '导出日志',     1),
    (7, 'message:write',      '消息写操作',   1)
ON DUPLICATE KEY UPDATE
    `perm_name` = VALUES(`perm_name`),
    `active`    = VALUES(`active`);

-- 用户组（group_id 1=member, 2=admin）
INSERT INTO `group` (`group_id`, `name`, `description`, `creator_user_id`, `is_admin`, `is_deleted`) VALUES
    (1, 'member', '普通用户组', NULL, 0, 0),
    (2, 'admin',  '系统管理员组', NULL, 1, 0)
ON DUPLICATE KEY UPDATE
    `description` = VALUES(`description`),
    `is_admin`    = VALUES(`is_admin`),
    `is_deleted`  = VALUES(`is_deleted`);

-- member 组：基础权限
INSERT INTO `group_permission` (`group_id`, `perm_id`, `status`) VALUES
    (1, 1, 'ACTIVE'),
    (1, 2, 'ACTIVE'),
    (1, 3, 'ACTIVE'),
    (1, 4, 'ACTIVE')
ON DUPLICATE KEY UPDATE `status` = 'ACTIVE';

-- admin 组：同样继承基础权限（admin 角色由 is_admin=1 + Shiro roles[admin] 判定）
INSERT INTO `group_permission` (`group_id`, `perm_id`, `status`) VALUES
    (2, 1, 'ACTIVE'),
    (2, 2, 'ACTIVE'),
    (2, 3, 'ACTIVE'),
    (2, 4, 'ACTIVE')
ON DUPLICATE KEY UPDATE `status` = 'ACTIVE';
