# 文档与实现缺口（GAPS）

> 轻量维护：记录 API/ADR 与代码差异。详细 log 域状态见 [API.md §5](API.md) 与 [ADR.md §6](ADR.md)。

## 1. 已知差异

| 项 | 文档 | 实现 | 状态 |
|----|------|------|------|
| 内部服务网络隔离 | 禁止直连 8081–8083 | compose expose only + K8s ClusterIP | done |
| 内部调用 token | topbiz → `/internal/*` | `InternalAuthFilter` + WebClient header | done |
| log 对外接口鉴权 | Shiro + admin | TopBiz `/api/v1/log/**` → `roles[admin]` | done |
| Admin/Message 响应包装 | 内部 `{code!=0}` 应映射为对外 error | `ServiceResultMapper` 统一 unwrap | done |
| 验证码 send/verify | message-service Redis | `VerificationCodeService` | done |
| 验证码登录 | `{credential, code}` 无 password | topbiz 验码 + user-service `codeVerified` | done |
| 欢迎信 templateId 硬编码 | 注册/管理员创建后 instant | `devops.messaging.welcome` + `WelcomeMessageService` | done |
| TopBiz/message 渠道枚举 | FEISHU/WECHAT 与后端不一致 | `common.message.MessageConstants` 统一 MVP 三渠道 | done |
| TENCENT_SMS instant / carrier test | 文档要求腾讯云 | message-service 返回 501；carrier test 仅 EMAIL | partial |
| 手机验证码 register/login | API.md 规划 | TopBiz 返回 **501**（未接第三方）；仅 PHONE 格式校验；PHONE+密码可注册/登录 | partial |
| 邮箱验证码注册 + 设密 | 验码后须提交 password | 统一 `POST /api/v1/register` `{email,code,password}`；仅 `{email,code}` 400 | done |
| user RBAC schema | status/created_at、login_history | `02a_user_rbac_migrate.sql` 对齐代码与 `02_devops_user.sql` | done |
| 注册默认组/权限 | member + 基础 perm；userId=1 → admin | `UserService.assignDefaultGroups` + `02b_user_rbac_seed.sql` | done |
| 对外注册/登录入口 | 仅 register/login | 已删除 `/register/email_code` 等子路径 | done |
| scheduled / variables CRUD | API.md 规划 | message-service 501 | partial |
| API.md message 端点 status | 大量 planned | 已同步 done/partial | done |
| topbiz Shiro 权限（admin + log 代理） | ShiroRealm + 路由 | `ShiroConfig`、`ShiroRealm`、`LogController` | done |
| POST ops/query 代理 | `POST /api/v1/log/ops/query` | TopBiz LogController | done |
| 预聚合 / metrics config | ADR §6.5 MVP | log-service 已实现 | done |
| 本地开发环境文档 | README + DEVELOPMENT + 变更汇总 | 云端 MySQL/Redis + 本地 CH/Vector | done |
| WebSocket 监控 | logservice §2.4 | 未实现 | 选修 |
| Flyway 迁移 | ADR §7 | 仍用手工 SQL | 待办 |

## 2. 管理员判定（dev）

- 首个注册用户 `userId=1` 自动加入 `admin` 组；`GET /api/v1/permissions` 返回 `roles` 含 `admin`、`is_admin: true`（需已执行 `02b_user_rbac_seed.sql`）。
- Shiro 对 `/api/v1/log/**`、`/api/v1/admin/**` 使用 `roles[admin]`。
