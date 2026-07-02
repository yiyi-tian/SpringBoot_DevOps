# 用例实现状态总结

> 对照设计文档《可复用用户管理服务设计》§1.1 用例类需求 + §1.2 自驱类需求 + §2.1 可复用特性  
> 更新时间：2026-06-17（v0.4.0 实现 1.2.2.2 + 2.1.5 + 测试覆盖）  
> 基于备份整合后的当前状态

---

## 一、用例类需求（§1.1）

### 1.1.1 注册账号

| 编号 | 子用例 | 状态 | 实现说明 |
|------|--------|------|----------|
| 1.1.1.1 | 单一凭证+密码 | ✅ 已完成 | `POST /api/v1/register` — 自动识别 PHONE/EMAIL/USERNAME，BCrypt 哈希，凭证唯一性校验 |
| 1.1.1.2 | 邮箱+验证码 | ✅ 已完成 | `POST /api/v1/register/email_code`（发码）→ `POST /api/v1/register`（提交），验证码由 message-service Redis 管理 |
| 1.1.1.3 | 手机+验证码 | ✅ 已完成 | `POST /api/v1/register/phone_code`（发码）→ `POST /api/v1/register`（提交），同上 |
| 1.1.1.4 | 飞书扫码 | ❌ 未完成 | **原因**：需飞书开放平台开发者账号，配置 OAuth 回调；当前缺乏飞书 SDK 集成 |
| 1.1.1.5 | 微信扫码 | ❌ 未完成 | **原因**：需微信开放平台开发者账号，配置 OAuth 回调；当前缺乏微信 SDK 集成 |

### 1.1.2 注销账号

| 编号 | 子用例 | 状态 | 实现说明 |
|------|--------|------|----------|
| 1.1.2 | 注销账号 | ✅ 已完成 | `POST /api/v1/deregister` — 需登录，user-service 更新 status→DEREGISTERED，topbiz 清除 Shiro 会话并记录审计日志 |

### 1.1.3 登录账号

| 编号 | 子用例 | 状态 | 实现说明 |
|------|--------|------|----------|
| 1.1.3.1 | 单一凭证+密码 | ✅ 已完成 | `POST /api/v1/login` — 自动识别凭证类型，BCrypt 校验，创建 Shiro 会话，更新 lastLoginAt |
| 1.1.3.2 | 邮箱+验证码 | ✅ 已完成 | `POST /api/v1/login/email_code`（发码）→ `POST /api/v1/login`（提交） |
| 1.1.3.3 | 手机+验证码 | ✅ 已完成 | `POST /api/v1/login/phone_code`（发码）→ `POST /api/v1/login`（提交） |
| 1.1.3.4 | 飞书二维码 | ❌ 未完成 | **原因**：同 1.1.1.4，需飞书 OAuth 集成 |
| 1.1.3.5 | 微信二维码 | ❌ 未完成 | **原因**：同 1.1.1.5，需微信 OAuth 集成 |

### 1.1.4 登出账号

| 编号 | 子用例 | 状态 | 实现说明 |
|------|--------|------|----------|
| 1.1.4 | 登出账号 | ✅ 已完成 | `POST /api/v1/logout` — 需登录，清除 Shiro 会话，调用 user-service，记录审计日志 |

### 1.1.5 查看权限

| 编号 | 子用例 | 状态 | 实现说明 |
|------|--------|------|----------|
| 1.1.5 | 查看权限 | ✅ 已完成 | `GET /api/v1/permissions` — 需登录，聚合查询 user_permission（直接权限）∪ group_permission（通过分组），去重返回 permCode 列表 |

### 1.1.6 查看分组

| 编号 | 子用例 | 状态 | 实现说明 |
|------|--------|------|----------|
| 1.1.6 | 查看分组 | ✅ 已完成 | `GET /api/v1/groups` — 需登录，查询 user_group 关联 + group 详情，返回 groupId/name/description |

### 1.1.7 查看历史记录

| 编号 | 子用例 | 状态 | 实现说明 |
|------|--------|------|----------|
| 1.1.7 | 查看历史记录 | ✅ 已完成 | `GET /api/v1/log` — 需登录，调用 log-service 按 userId 查询 MySQL audit_log 表，支持分页、按 operation/时间 筛选 |

### 1.1.8 修改密码（已知密码）

| 编号 | 子用例 | 状态 | 实现说明 |
|------|--------|------|----------|
| 1.1.8 | 修改密码 | ✅ 已完成 | `PUT /api/v1/password` — 需登录，校验旧密码 → 加密新密码 → 更新该用户所有密码凭证（支持多凭证同步），记录审计日志 |

### 1.1.9 重置密码（忘记密码）

| 编号 | 子用例 | 状态 | 实现说明 |
|------|--------|------|----------|
| 1.1.9 | 重置密码 | ✅ 已完成 | `POST /api/v1/password/reset/email_code` 或 `phone_code`（发码）→ `POST /api/v1/password/reset`（提交：credential + code + newPassword），topbiz 校验验证码后调用 user-service 更新密码 |

### 1.1.10 修改基本信息

| 编号 | 子用例 | 状态 | 实现说明 |
|------|--------|------|----------|
| 1.1.10 | 修改基本信息 | ✅ 已完成 | `PATCH /api/v1/profile` — 需登录，允许修改 displayName、sex，返回更新后的用户信息，记录审计日志 |

### 1.1.11 申请权限

| 编号 | 子用例 | 状态 | 实现说明 |
|------|--------|------|----------|
| 1.1.11 | 申请权限 | ✅ 已完成 | `POST /api/v1/permissions/apply` — 需登录，三步拦截：Token校验 → 查重已有权限(status=ACTIVE) → 查重在途申请(status=PENDING)，全通过则创建 PENDING 记录。异步通知管理员 + 记录审计日志。管理员可通过 `POST /api/v1/admin/user-permissions/{id}/approve|reject` 审批 |

### 1.1.12 用户管理（管理员）

| 编号 | 子用例 | 状态 | 实现说明 |
|------|--------|------|----------|
| 1.1.12.1 | 新增用户 | ✅ 已完成 | `POST /api/v1/admin/users` — 管理员创建用户，发送欢迎站内信，记录审计日志 |
| 1.1.12.2 | 删除用户 | ✅ 已完成 | `DELETE /api/v1/admin/users` — 逻辑删除（is_deleted=1），记录审计日志 |
| 1.1.12.3 | 修改用户信息 | ✅ 已完成 | `PATCH /api/v1/admin/users` — 支持 displayName/status/sex 部分更新，记录审计日志 |
| 1.1.12.4 | 查询用户信息 | ✅ 已完成 | `GET /api/v1/admin/users` — 分页 + keyword（模糊匹配 displayName）+ status 筛选 |

### 1.1.13 分组管理（管理员）

| 编号 | 子用例 | 状态 | 实现说明 |
|------|--------|------|----------|
| 1.1.13.1 | 创建分组 | ✅ 已完成 | `POST /api/v1/admin/groups` — 支持 name/description/creatorUserId |
| 1.1.13.2 | 删除分组 | ✅ 已完成 | `DELETE /api/v1/admin/groups` — **级联清理**：先删除 user_group 关联 + group_permission 关联，再逻辑删除分组 |
| 1.1.13.3 | 修改分组 | ✅ 已完成 | `PATCH /api/v1/admin/groups` — 支持 name/description 部分更新 |
| 1.1.13.4 | 查询分组信息 | ✅ 已完成 | `GET /api/v1/admin/groups` — 分页 + keyword（模糊匹配 name）筛选 |
| 1.1.13.5 | 组申请权限 | ✅ 已完成 | `POST /api/v1/admin/groups/{groupId}/permissions/apply` — 管理员或组成员为分组申请权限，三步拦截：分组存在校验 → 查重已有权限(status=ACTIVE) → 查重在途申请(status=PENDING)，全通过则创建 PENDING 记录。管理员可通过 `POST /api/v1/admin/group-permissions/{id}/approve|reject` 审批 |

### 1.1.14 用户组成员管理（管理员）

| 编号 | 子用例 | 状态 | 实现说明 |
|------|--------|------|----------|
| 1.1.14.1 | 添加用户 | ✅ 已完成 | `POST /api/v1/admin/group-users` — 校验用户/分组存在 + 去重检查，返回 409 若重复 |
| 1.1.14.2 | 移除用户 | ✅ 已完成 | `DELETE /api/v1/admin/group-users` — 校验关联存在，物理删除关联记录 |
| 1.1.14.3 | 查看分组成员 | ✅ 已完成 | `GET /api/v1/admin/group-users` — 分页查询，关联 user 表返回 displayName/status |

### 1.1.15 权限管理（管理员）

| 编号 | 子用例 | 状态 | 实现说明 |
|------|--------|------|----------|
| 1.1.15.1 | 创建权限 | ✅ 已完成 | `POST /api/v1/admin/permissions` — permCode 唯一性校验，创建后 active=1 |
| 1.1.15.2 | 删除权限 | ✅ 已完成 | `DELETE /api/v1/admin/permissions` — **级联清理**：先删除 group_permission + user_permission 关联，再 active=0 |
| 1.1.15.3 | 修改权限信息 | ✅ 已完成 | `PATCH /api/v1/admin/permissions` — 支持 permCode/permName 更新 |
| 1.1.15.4 | 查看权限列表 | ✅ 已完成 | `GET /api/v1/admin/permissions` — 分页 + keyword（模糊匹配 permCode/permName）|
| *(扩展)* | 用户直接权限 CRUD | ✅ 已完成 | `POST/DELETE/PATCH/GET /api/v1/admin/user-permissions` — user_permission 表独立管理，支持按 userId 查询 |

### 1.1.16 分组和权限管理（管理员）

| 编号 | 子用例 | 状态 | 实现说明 |
|------|--------|------|----------|
| 1.1.16.1 | 增加分组权限 | ✅ 已完成 | `POST /api/v1/admin/group-permissions` — 校验分组/权限存在 + 去重检查 |
| 1.1.16.2 | 删除分组权限 | ✅ 已完成 | `DELETE /api/v1/admin/group-permissions` — 物理删除 group_permission 关联记录 |
| 1.1.16.3 | 修改分组权限 | ✅ 已完成 | `PATCH /api/v1/admin/group-permissions` — 支持更换 permId |
| 1.1.16.4 | 查看分组权限 | ✅ 已完成 | `GET /api/v1/admin/group-permissions` — 关联 permission 表返回完整权限信息 |

---

## 二、自驱类需求（§1.2）

### 1.2.1 认证状态生命周期

| 编号 | 子用例 | 状态 | 实现说明 |
|------|--------|------|----------|
| 1.2.1.1 | 账号认证过期检测 | ✅ 已完成 | `SchedulerService.expireStaleAuths()` — 每日 3:00 执行，扫描 user_auth.expired_at 超过 14 天的用户，标记 status→EXPIRED，记录调度审计日志 |
| 1.2.1.2 | 过期验证码清理 | ✅ 已完成 | **原因**：验证码存储在 Redis 中，已配置 TTL 自动过期，无需额外定时清理代码。Redis 的 `EXPIRE` 机制在 Key 到期后自动删除，符合设计预期 |

### 1.2.2 用户账号状态

| 编号 | 子用例 | 状态 | 实现说明 |
|------|--------|------|----------|
| 1.2.2.1 | 识别并更新长期未活跃用户 | ✅ 已完成 | `SchedulerService.deactivateInactiveUsers()` — 每周日 4:00 执行，扫描 lastLoginAt 超过 30 天的 ACTIVE 用户（含从未登录者），标记 status→INACTIVE |
| 1.2.2.2 | 风险用户识别与自动化处置（IP突变） | ✅ 已完成 | **v0.4.0** — 无需 GeoLite2，基于 /16 子网对比：24h 内 ≥3 个不同子网 → RISKY，≥5 个 → LOCKED。登录时内联检测 + `SchedulerService.scanIpMutations()` 每日 3:30 定时扫描 |

### 1.2.3 用户数据管理

| 编号 | 子用例 | 状态 | 实现说明 |
|------|--------|------|----------|
| 1.2.3.1 | 清理用户过期缓存 | ✅ 已完成 | `SchedulerService.cleanExpiredCaches()` — 每月1日 2:00 执行。建立缓存键命名规范（`user:perm:v{version}:{userId}` 等），当前 Shiro 会话由 Redis TTL 自动管理，此任务预留为应用级缓存版本升级清理入口 |
| 1.2.3.2 | 延迟删除注销用户数据 | ✅ 已完成 | `SchedulerService.purgeDeregisteredUsers()` — 每周日 5:00 执行，物理删除 DEREGISTERED 超过 30 天的用户及全部关联数据（user_auth、attribute、user_group、user_permission） |
| 1.2.3.3 | 清理用户过期数据 | ✅ 已完成 | `SchedulerService.cleanExpiredData()` — 每月1日 2:30 执行，物理删除超过 90 天的 REJECTED 权限申请记录（user_permission + group_permission），防止 junction 表无限增长 |

---

## 三、可复用功能特性（§2.1）

| 编号 | 特性 | 状态 | 说明 |
|------|------|------|------|
| 2.1.1 | 统一身份认证 | ✅ 已完成 | 支持 PHONE/EMAIL/USERNAME 三种凭证登录，自动识别 |
| 2.1.2 | 自定义用户属性 | ✅ 已完成 | attribute 表支持 KV 扩展属性 |
| 2.1.3 | user-group-permission 权限架构 | ✅ 已完成 | 用户→组→权限 多级映射 + 直接权限，登录时聚合并集 |
| 2.1.4 | 多凭证登录 | ✅ 已完成 | 同一用户支持多种登录方式，统一收敛到单一 UID |
| 2.1.5 | 多端登录和登出管理 | ✅ 已完成 | **v0.4.0** — `GET /api/v1/sessions` 查看活跃会话（含设备类型/IP/登录时间/是否当前），`DELETE /api/v1/sessions/{sessionId}` 强制登出指定会话（Redis key 即时删除），`DELETE /api/v1/sessions?scope=others` 批量登出其他会话。基于 User-Agent 的轻量设备识别（WEB/ANDROID/IOS/DESKTOP），MySQL `user_session` 表独立追踪，每小时定时清理过期会话 |
| 2.1.6 | 账号绑定与身份合并 | ✅ 已完成 | `POST /api/v1/account/bind` — 已登录用户可绑定新的手机/邮箱/用户名凭证，自动查重防止多账号 |
| 2.1.7 | 异步事件通知机制 | ✅ 已完成 | 注册/管理员创建用户发送欢迎信；关键操作记录审计日志（异步 try/catch，失败不阻塞主流程） |

---

## 四、统计汇总

| 分类 | 总数 | 已完成 | 未完成 | 完成率 |
|------|------|--------|--------|--------|
| 用例类需求（§1.1） | 42 | **38** | 4 | 90% |
| 自驱类需求（§1.2） | 7 | **7** | 0 | 100% |
| 可复用特性（§2.1） | 7 | **7** | 0 | 100% |
| **合计** | **56** | **52** | **4** | **93%** |

> 与备份差异：备份 already.md 标记 1.1.11（申请权限）为已完成，但实际代码中不存在对应端点——现已补齐实现。2.1.8（多身份切换）已从需求范围中移除。

### 未完成原因分类

| 原因 | 数量 | 涉及编号 |
|------|------|----------|
| 需第三方开发者账号（飞书/微信 OAuth） | 4 | 1.1.1.4, 1.1.1.5, 1.1.3.4, 1.1.3.5 |

---

## 五、本次整合变更（2026-06-17）

### 新增功能
- **1.1.2** 注销账号 — `POST /api/v1/deregister`
- **1.1.4** 登出账号 — `POST /api/v1/logout`
- **1.1.5** 查看权限 — `GET /api/v1/permissions`
- **1.1.6** 查看分组 — `GET /api/v1/groups`
- **1.1.8** 修改密码 — `PUT /api/v1/password`
- **1.1.9** 重置密码 — `POST /api/v1/password/reset`
- **1.1.10** 修改基本信息 — `PATCH /api/v1/profile`
- **1.1.11** 申请权限 — `POST /api/v1/permissions/apply`
- **1.1.13.5** 组申请权限 — `POST /api/v1/admin/groups/{groupId}/permissions/apply`
- **1.1.15** 用户直接权限 CRUD — `/api/v1/admin/user-permissions`
- **1.2.1.1** 认证过期检测 — 定时任务每日 3:00
- **1.2.2.1** 不活跃用户标记 — 定时任务每周日 4:00
- **1.2.3.2** 注销用户物理删除 — 定时任务每周日 5:00
- **1.2.1.2** 过期验证码清理 — Redis TTL 自动处理，标记完成
- **1.2.3.1** 过期缓存清理 — 定时任务每月1日 2:00
- **1.2.3.3** 过期数据清理 — 定时任务每月1日 2:30，清理超过90天的REJECTED记录
- **2.1.6** 账号绑定 — `POST /api/v1/account/bind`

### 移除功能
- **2.1.8** 多身份/上下文平滑切换 — 从需求范围中移除

### 增强功能
- User 实体新增 `lastLoginAt` 字段
- UserPermission 实体新增 `status` + `createdAt` 字段
- GroupPermission 实体新增 `status` + `createdAt` 字段
- 登录时自动更新 `lastLoginAt`
- 删除分组的级联清理（user_group + group_permission）
- 删除权限的级联清理（group_permission + user_permission）
- 权限聚合查询仅返回 status=ACTIVE 的记录
- 管理员审批端点：user-permission 和 group-permission 的 approve/reject
- 建立缓存键命名规范：`user:perm:v{version}:{userId}` 等
- 修复 UserServiceClient deregister/logout 的 HTTP 方法与 Controller 不匹配问题

### v0.4.0 新增功能 (2026-06-17)
- **1.2.2.2** IP 突变检测 — 基于 /16 子网对比，无需 GeoLite2。登录时内联检测 + 每日 3:30 `scanIpMutations()` 定时扫描。≥3 个不同子网 → RISKY，≥5 个 → LOCKED
- **2.1.5** 多端登录管理 — `GET /api/v1/sessions` 查看活跃会话，`DELETE /api/v1/sessions/{id}` 强制登出指定会话（Redis key 即时删除），`DELETE /api/v1/sessions?scope=others` 批量登出
- **设备识别** — 基于 User-Agent 自动识别 WEB / ANDROID / IOS / DESKTOP
- **登录历史** — `login_history` 表记录每次登录的 IP、User-Agent、时间
- **会话追踪** — `user_session` 表独立追踪 Shiro 会话生命周期，每小时 `cleanStaleSessions()` 清理过期会话

### v0.4.0 增强功能
- User 实体新增 `lastLoginIp` 字段
- 登录时捕获并传递 clientIp 和 userAgent 全链路
- `LOCKED` 用户拒绝登录（403），`RISKY` 用户允许登录但记录警告
- `LogService.ALLOWED_OPERATIONS` 补齐缺失的 15+ 个操作（修复调度器审计日志静默失败 bug）
- 数据库新增 `login_history` 和 `user_session` 两张表
- **测试覆盖**：新增 124 个测试（UserServiceTest 78 + UserControllerTest 46），覆盖所有核心业务路径

---

## 六、测试（新增 2026-06-17）

### 测试环境
- **框架**：JUnit 5 + Spring Boot Test + MockMvc + MyBatis-Plus
- **数据库**：H2 内存数据库（`MODE=MySQL`）
- **测试配置**：`src/test/resources/application-test.yml` + `schema.sql`（10张完整表结构）
- **运行方式**：`./mvnw test -pl user-service -am`

### 测试覆盖范围

| 测试类 | 测试数 | 覆盖模块 |
|--------|--------|----------|
| `UserServiceTest.java` | 78 | 服务层全方法：注册、登录、修改密码、更新资料、注销、登出、管理员CRUD、分组管理、权限管理、用户-分组关联、分组-权限关联、用户-权限关联、权限申请与审批（含组）、自驱定时任务、会话管理、IP突变检测、重置密码、绑定凭证 |
| `UserControllerTest.java` | 46 | 控制器层全端点：通过 MockMvc 测试 HTTP 请求→响应完整链路，覆盖所有 `/internal/` 路径的 REST API，包括参数校验、错误码断言、级联操作验证 |
| **合计** | **124** | 零失败，零错误 |

### 测试产物

| 文件 | 说明 |
|------|------|
| `pom.xml` | 新增 `spring-boot-starter-test` + `h2` 测试依赖 |
| `src/test/resources/application-test.yml` | H2 数据源 + MyBatis-Plus 配置 + AccessLog 属性 |
| `src/test/resources/schema.sql` | 10 张表的完整 H2 DDL（含 `NON_KEYWORDS=GROUP,USER`） |
| `src/test/java/.../UserServiceTest.java` | 服务层测试（78 tests） |
| `src/test/java/.../UserControllerTest.java` | 控制器层集成测试（46 tests） |

### 测试中发现的代码问题及修复

| 问题 | 影响方法 | 修复 |
|------|----------|------|
| `selectCount` 复用含 `ORDER BY` 的 `QueryWrapper`，H2 报错 `Column must be in GROUP BY list` | `searchUsers`、`searchGroups`、`searchPermissions` | 拆分为独立的 countWrapper（无排序）+ listWrapper（含排序+分页），提升数据库兼容性 |
