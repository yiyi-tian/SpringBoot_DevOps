# user-service 数据库设计

> 数据库：MySQL `devops_user`  
> 建表脚本：[docs/sql/02_devops_user.sql](../docs/sql/02_devops_user.sql)  
> 更新时间：2026-06-17

---

## 1. 设计原则

| 原则 | 说明 |
|------|------|
| **凭证与档案分离** | 登录标识放 `user_auth`；`user` 存主体档案 |
| **不存 phone/email/password 在 user 表** | 统一走 `user_auth`，支持多凭证 |
| **权限两级聚合** | 有效权限 = `user_permission`（直接）∪ 用户所属 `group` 的 `group_permission`（间接），去重 `perm_code` |
| **逻辑删除优先** | user/group 使用 `is_deleted` 软标记；permission 使用 `active` 启停 |

---

## 2. ER 图

```
user ──1:N── user_auth        （认证凭证）
user ──1:N── attribute         （扩展属性）
user ──1:N── user_group ──N:1── group
user ──1:N── user_permission ──N:1── permission
group ──1:N── group_permission ──N:1── permission
```

---

## 3. 表结构

### 3.1 `user`（用户主体）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `user_id` | BIGINT PK | 是 | AUTO | 用户唯一标识 |
| `display_name` | VARCHAR(128) | 是 | — | 显示名 |
| `sex` | TINYINT | 否 | NULL | 性别 |
| `status` | VARCHAR(32) | 是 | `'ACTIVE'` | ACTIVE / LOCKED / DEREGISTERED / EXPIRED / INACTIVE |
| `is_deleted` | TINYINT(1) | 是 | 0 | 逻辑删除标记 |
| `last_login_at` | DATETIME | 否 | NULL | 最后登录时间 |
| `created_at` | DATETIME | 是 | NOW | 创建时间 |
| `updated_at` | DATETIME | 是 | NOW | 更新时间（自动更新） |

**索引**：`idx_status`, `idx_is_deleted`

**对应 Entity**：[User.java](src/main/java/org/example/userservice/entity/User.java)

### 3.2 `user_auth`（认证凭证）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `auth_id` | BIGINT PK | 是 | AUTO | 凭证唯一标识 |
| `user_id` | BIGINT | 是 | — | 关联 user.user_id |
| `identity_type` | VARCHAR(32) | 是 | — | PHONE / EMAIL / USERNAME / FEISHU / WECHAT |
| `identifier` | VARCHAR(255) | 是 | — | 手机号/邮箱/用户名/OpenID |
| `secret_hash` | VARCHAR(255) | 是 | — | BCrypt 密码哈希（OAuth 可空） |
| `verified` | TINYINT(1) | 是 | 1 | 是否已验证 |
| `expired_at` | DATETIME | 否 | NULL | 凭证过期时间 |
| `created_at` | DATETIME | 是 | NOW | 创建时间 |

**唯一约束**：`uk_identity` (`identity_type`, `identifier`)  
**外键**：`user_id` → `user.user_id` (ON DELETE RESTRICT)

**对应 Entity**：[UserAuth.java](src/main/java/org/example/userservice/entity/UserAuth.java)

### 3.3 `attribute`（用户扩展属性）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `attr_id` | BIGINT PK | 是 | AUTO | 属性记录唯一标识 |
| `user_id` | BIGINT | 否 | NULL | 关联 user.user_id |
| `attr_key` | VARCHAR(64) | 是 | — | 属性键名 |
| `attr_value` | VARCHAR(512) | 是 | — | 属性值 |

**外键**：`user_id` → `user.user_id` (ON DELETE SET NULL)

**对应 Entity**：[Attribute.java](src/main/java/org/example/userservice/entity/Attribute.java)

### 3.4 `group`（用户组/角色）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `group_id` | BIGINT PK | 是 | AUTO | 组唯一标识 |
| `name` | VARCHAR(255) | 是 | — | 组名称 |
| `description` | VARCHAR(255) | 否 | NULL | 组描述 |
| `creator_user_id` | BIGINT | 否 | NULL | 创建者用户ID |
| `is_admin` | TINYINT(1) | 是 | 0 | 是否管理员组 |
| `is_deleted` | TINYINT(1) | 是 | 0 | 逻辑删除标记 |
| `created_at` | DATETIME | 是 | NOW | 创建时间 |

**外键**：`creator_user_id` → `user.user_id` (ON DELETE SET NULL)

**对应 Entity**：[Group.java](src/main/java/org/example/userservice/entity/Group.java)

### 3.5 `permission`（权限）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `perm_id` | BIGINT PK | 是 | AUTO | 权限唯一标识 |
| `perm_code` | VARCHAR(255) | 是 | — | 权限编码，如 `user:add` |
| `perm_name` | VARCHAR(255) | 是 | — | 权限名称 |
| `active` | TINYINT(1) | 是 | 1 | 是否启用：0-禁用，1-启用 |

**唯一约束**：`uk_perm_code` (`perm_code`)

**对应 Entity**：[Permission.java](src/main/java/org/example/userservice/entity/Permission.java)

### 3.6 `user_group`（用户-组关联）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | BIGINT PK | 是 | 关联记录唯一标识 |
| `user_id` | BIGINT | 是 | 关联 user.user_id |
| `group_id` | BIGINT | 是 | 关联 group.group_id |

**唯一约束**：`uk_user_group` (`user_id`, `group_id`)  
**外键**：`user_id` → `user` (CASCADE)、`group_id` → `group` (CASCADE)

**对应 Entity**：[UserGroup.java](src/main/java/org/example/userservice/entity/UserGroup.java)

### 3.7 `group_permission`（组-权限关联）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | BIGINT PK | 是 | AUTO | 关联记录唯一标识 |
| `group_id` | BIGINT | 是 | — | 关联 group.group_id |
| `perm_id` | BIGINT | 是 | — | 关联 permission.perm_id |
| `status` | VARCHAR(32) | 是 | `'PENDING'` | ACTIVE-已授权 / PENDING-待审批 / REJECTED-已驳回（默认PENDING，最小权限原则） |
| `created_at` | DATETIME | 是 | NOW | 创建时间 |

**唯一约束**：`uk_group_permission` (`group_id`, `perm_id`)  
**外键**：`group_id` → `group` (CASCADE)、`perm_id` → `permission` (CASCADE)  
**索引**：`idx_status` (`status`)

**对应 Entity**：[GroupPermission.java](src/main/java/org/example/userservice/entity/GroupPermission.java)

### 3.8 `user_permission`（用户-权限直接关联）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | BIGINT PK | 是 | AUTO | 关联记录唯一标识 |
| `user_id` | BIGINT | 是 | — | 关联 user.user_id |
| `perm_id` | BIGINT | 是 | — | 关联 permission.perm_id |
| `status` | VARCHAR(32) | 是 | `'PENDING'` | ACTIVE-已授权 / PENDING-待审批 / REJECTED-已驳回（默认PENDING，最小权限原则） |
| `created_at` | DATETIME | 是 | NOW | 创建时间 |

**唯一约束**：`uk_user_permission` (`user_id`, `perm_id`)  
**外键**：`user_id` → `user` (CASCADE)、`perm_id` → `permission` (CASCADE)  
**索引**：`idx_status` (`status`)

**对应 Entity**：[UserPermission.java](src/main/java/org/example/userservice/entity/UserPermission.java)

---

## 4. 状态流转

### 4.1 用户状态流转

```
注册 ──→ ACTIVE
           │
           ├── 30天未登录 ──→ INACTIVE（SchedulerService 每周日 4:00）
           │
           ├── 凭证过期14天 ──→ EXPIRED（SchedulerService 每日 3:00）
           │
           └── 用户主动注销 ──→ DEREGISTERED
                                  │
                                  └── 30天后 ──→ 物理删除（SchedulerService 每周日 5:00）
```

### 4.2 权限申请状态流转

```
管理员直接授权 ──→ ACTIVE ───────────────────────────────→ 参与权限聚合
                      ↑
用户/组申请 ──→ PENDING ──┬── 管理员审批通过 ──→ ACTIVE ──→ 参与权限聚合
                         │
                         └── 管理员驳回 ──→ REJECTED ──→ 保留记录，不参与聚合
```

- `ACTIVE`：管理员直接授权或审批通过后的生效状态，`getPermissions()` 仅聚合此状态的权限
- `PENDING`：用户/组提交申请后的在途状态，不授予实际权限
- `REJECTED`：管理员驳回的终态，保留审计记录

---

## 5. 与 API 映射

| API（internal） | 主要操作表 |
|-----------------|-----------|
| `/internal/user/register` | user + user_auth |
| `/internal/user/login` | user_auth → 更新 user.last_login_at |
| `/internal/user/{userId}/deregister` | user (status→DEREGISTERED) |
| `/internal/user/{userId}/logout` | 无 DB 操作（Shiro 会话管理） |
| `/internal/user/{userId}/permissions` | user_permission + group_permission + user_group |
| `/internal/user/{userId}/groups` | user_group + group |
| `/internal/user/password` | user_auth |
| `/internal/user/profile` | user |
| `/internal/user/password/reset` | user_auth |
| `/internal/user/bind` | user_auth |
| `/internal/user/create` | user + user_auth |
| `/internal/user/delete` | user (is_deleted=1) |
| `/internal/user/update` | user |
| `/internal/user/search` | user |
| `/internal/group/*` | group (+ user_group/group_permission 级联) |
| `/internal/group-user/*` | user_group |
| `/internal/permission/*` | permission (+ group_permission/user_permission 级联) |
| `/internal/group-permission/*` | group_permission |
| `/internal/user-permission/*` | user_permission |
| `/internal/scheduler/expire-stale-auths` | user_auth → user.status |
| `/internal/scheduler/deactivate-inactive-users` | user (last_login_at → status) |
| `/internal/scheduler/purge-deregistered-users` | user + 全部关联表物理删除 |
| `/internal/user-permission/apply` | user_permission (status=PENDING) |
| `/internal/user-permission/approve` | user_permission (status: PENDING→ACTIVE) |
| `/internal/user-permission/reject` | user_permission (status: PENDING→REJECTED) |
| `/internal/group-permission/apply` | group_permission (status=PENDING) |
| `/internal/group-permission/approve` | group_permission (status: PENDING→ACTIVE) |
| `/internal/group-permission/reject` | group_permission (status: PENDING→REJECTED) |
| `/internal/scheduler/clean-expired-caches` | 无直接 DB 操作（缓存键规范管理） |
| `/internal/scheduler/clean-expired-data` | user_permission + group_permission（物理删除过期 REJECTED 记录） |

---

## 6. 相关文档

- [docs/sql/02_devops_user.sql](../docs/sql/02_devops_user.sql) — 建表脚本
- [docs/DATA_MODEL.md](../docs/DATA_MODEL.md) — 项目级数据模型设计
- [docs/API.md](../docs/API.md) — 接口规格
- [already.md](already.md) — 用例实现状态
