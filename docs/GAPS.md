# 文档与实现缺口（GAPS）

> 轻量维护：记录 API/ADR 与代码差异。详细 log 域状态见 [API.md §5](API.md) 与 [ADR.md §6](ADR.md)。

## 1. 已知差异

| 项 | 文档 | 实现 | 状态 |
|----|------|------|------|
| log 对外接口鉴权 | Shiro + admin | TopBiz `/api/v1/log/**` → `roles[admin]` | done |
| topbiz Shiro 权限（admin + log 代理） | ShiroRealm + 路由 | `ShiroConfig`、`ShiroRealm`、`LogController` | done |
| POST ops/query 代理 | `POST /api/v1/log/ops/query` | TopBiz LogController | done |
| 预聚合 / metrics config | ADR §6.5 MVP | log-service 已实现 | done |
| 本地开发环境文档 | README + DEVELOPMENT + 变更汇总 | 云端 MySQL/Redis + 本地 CH/Vector | done |
| WebSocket 监控 | logservice §2.4 | 未实现 | 选修 |
| Flyway 迁移 | ADR §7 | 仍用手工 SQL | 待办 |

## 2. 管理员判定（dev）

- `user-service GET /internal/user/{userId}/permissions`：`userId=1` 或 `identifier=admin` 返回 `roles: ["admin"]`。
