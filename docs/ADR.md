# 技术决策记录（ADR）

> 开发前定稿。业务接口见 [API.md](API.md)、[DATA_MODEL.md](DATA_MODEL.md)。  
> **日志域以 [logservice.md](../logservice.md) 为需求原文**；本节为可执行决策摘要。

| 文档 | 范围 |
|------|------|
| [API.md §1.6、§5](API.md) | 对外/内部 REST |
| [DATA_MODEL.md §4](DATA_MODEL.md) | `access_log`、`audit_log`、选修聚合表 |

## 1. HTTP 与响应协议

| 决策 | 内容 |
|------|------|
| 对外 Client → TopBiz | `Content-Type: application/json`，统一响应 `{"code":0,"message":"ok","data":{}}` |
| TopBiz → internal | 同上 JSON；错误码与 message 在各服务复用 `common` 模块 |
| 路径 | 对外必须以 `/api/v1/` 开头（禁止省略前导 `/`） |

## 2. 安全与认证

| 决策 | 内容 |
|------|------|
| 会话 | **Apache Shiro**（仅 TopBiz），Cookie `JSESSIONID`；**不用 JWT** |
| Session 存储 | **Redis**（Spring Session Data Redis，namespace `shiro:session`，检索 `shiro:session:*`），支持多实例 TopBiz；**本地 dev 连云端 Redis**，见各服务 `application-dev.yml` |
| 密码 | **BCrypt**（`spring-security-crypto`）；禁止新数据使用 MD5 |
| 内部服务 8081–8083 | 课程阶段信任本机网络；生产需 mTLS 或内网 Token（文档约定，暂不实现） |

## 3. 数据与枚举

| 决策 | 内容 |
|------|------|
| 分库 | `devops_user` / `devops_message` / `devops_log`（同 MySQL 实例） |
| 用户凭证 | 仅存 `user_auth`；`user` 表不存 phone/password |
| `channel_type` | **`IN_APP` \| `TENCENT_SMS` \| `EMAIL`**（与 API.md 一致；选修 FEISHU/WECHAT 预留） |
| 审计 operation | 大写英文：`USER_REGISTER`、`USER_LOGIN`、`ADMIN_USER_DELETE` 等 |

## 4. 消息发送边界

| 场景 | 接口 | 说明 |
|------|------|------|
| 验证码 | `POST /internal/message/email_code/send`、`phone_code/send` | 存 **Redis**，不走 instant |
| 业务通知（欢迎信等） | `POST /internal/messages/instant` | 必填 `templateId`；种子模板见 `docs/sql/02_devops_message_seed.sql` |
| 简易通知 | `POST /internal/message/send` | **已废弃（deprecated）**；新代码勿用，注册欢迎信改 instant |

## 5. Redis Key 规范

| 用途 | Key 模式 | TTL |
|------|----------|-----|
| 邮箱验证码 | `verify:email:{scene}:{email}` | 300s |
| 手机验证码 | `verify:phone:{scene}:{phone}` | 300s |
| 发送限流 | `verify:rate:{target}` | 60s 内最多 1 次 |
| 载体缓存 | `carrier:config:{carrierId}` | 3600s |
| Shiro Session | `shiro:session:sessions:{sessionId}`（及 `shiro:session:sessions:expires:*`、`shiro:session:expirations:*` 辅助 key；检索 `shiro:session:*`） | 与 session 超时一致（默认 30min） |

`scene`：`REGISTER` | `LOGIN`。

## 6. 日志（对齐 logservice.md）

### 6.1 双管道

| 管道 | 产生方式 | 存储 | 每次 HTTP 是否写 |
|------|----------|------|------------------|
| **访问/运维日志** | 四服务 `AccessLogInterceptor` → 本地 JSON | **Vector** → ClickHouse `devops.access_log` | 自动（拦截器） |
| **业务审计** | TopBiz 编排成功 → `POST /internal/log/record` | MySQL `audit_log` | 仅关键业务事件 |

与 logservice §5.1「审计拦截器」概念对齐：**课程实现为 BFF 编排 + record API**，不在 user/message 内嵌独立 `AuditInterceptor`。

### 6.2 访问日志 JSON（canonical：logservice §4）

**字段命名一律 snake_case**。logservice §5.1 样例中的 camelCase（如 `traceId`）仅作示意，**禁止写入 ClickHouse**。

| 字段 | 说明 |
|------|------|
| `trace_id` | 全链路 ID，`X-Trace-Id` 透传 |
| `service_name` | `topbiz` \| `user` \| `message` \| `log` |
| `client_ip` | 客户端 IP |
| `method` | HTTP 方法 |
| `uri` | 路径（含 `/api/v1/*`、`/internal/*`） |
| `cost_ms` | 耗时 |
| `http_status` | HTTP 状态码 |
| `biz_code` | 业务 code（如 `0`）；无则 `null` |
| `timestamp` | 毫秒时间戳 |
| `req_params` | 请求参数 JSON（脱敏后） |
| `res_body` | 响应体 JSON（脱敏后） |
| `level`（可选） | 应用日志级别，若采集 |

**`req_params` / `res_body` 记录策略（拍板 logservice §4「待定」）**：

| 配置项 | MVP 默认 |
|--------|----------|
| 是否记录 body | `devops.access-log.log-body=true` |
| 仅失败时记全量 | `devops.access-log.body-on-error-only=true`（`http_status>=400` 或 `biz_code!=0`） |
| 最大长度 | 单字段 ≤ 4KB，超出截断 |

**慢请求**：`cost_ms` > **3000**（可配置）时，应用日志打 **WARN**（logservice §5.1）。

### 6.3 拦截器职责（logservice §5）

| 能力 | MVP | 实现落点 |
|------|-----|----------|
| 链路 `X-Trace-Id` + MDC | 必做 | `common`：`TraceIdFilter`、HTTP Interface（WebClient）透传 |
| 访问日志 | 必做 | `common`：`AccessLogInterceptor` |
| 业务审计 | 必做 | TopBiz → `POST /internal/log/record` |
| 数据脱敏 | 必做 | 拦截器 mask + 审计 `detail`（手机/邮箱/密码/token） |
| 异常分类日志 | 建议 | `GlobalExceptionHandler` 扩展 `error_type`：`BIZ`/`SYS`/`RPC`/`DB`/`AUTH` |
| 事件化/Kafka、采样限流、Schema 校验、动态 DEBUG、多语言 SDK | 选修 | 见 §8 |

### 6.4 采集与基础设施（§2.1–§2.2、§5.4）

| 项 | 决策 |
|----|------|
| 采集工具 | **Vector**（**不用 Filebeat**；见 `infra/vector/vector.toml`） |
| 源目录 | IDE：`shared-logs/access/{serviceName}/`；Docker：`logs/access/{serviceName}/`；local compose 将 `shared-logs/access` 挂载为 Vector 的 `/var/log/access` |
| 触发 | Tail 文件事件；批量/超时刷新（§2.2） |
| 本地清理 | 日志框架按保留时长/体积删除旧 JSON（§2.1） |
| ClickHouse | 表 TTL **90 天**（`docs/sql/04_clickhouse_access_log.sql`） |

### 6.5 指标与查询（§1.1–§1.4、§3）

**MVP REST**（查询时聚合，§3.7 右列）：

| 能力 | 路径 | logservice |
|------|------|------------|
| 原始日志检索 | `GET/POST /internal/log/ops/query` | §1.1 |
| 指标查询 | `GET /internal/log/metrics`（`source=raw\|aggregate`） | §1.2 |
| 日志导出 | `POST /internal/log/ops/export` | §1.4 |
| 定时预聚合 | `metrics_aggregate` + `@Scheduled` | §2.3 |

**`metric` 参数（与 §3 对齐，MVP 可先实现子集）**：

| 分类 | 可选 `metric` 值 |
|------|------------------|
| 请求量 §3.1 | `qps`、`pv`、`api_calls`、`slow_count` |
| 错误 §3.2 | `error_rate`、`error_count`、`http_5xx`、`http_4xx` |
| 性能 §3.3 | `avg`、`p95`、`p99`、`max`、`slowest_api` |
| 稳定性 §3.4 | `success_rate` |
| 安全 §3.5 | `ip_request_topn`、`ip_error_topn` |

公共查询维度：`service_name`、`api`（uri）、`start_time`、`end_time`、可选 `top_n`、`interval`。

**选修（非 MVP）**：

| 能力 | logservice | 说明 |
|------|------------|------|
| 指标阈值配置 | §1.3 | `GET/PUT /internal/log/metrics/config` |
| WebSocket 监控推送 | §2.4 | 周期同步 + 阈值告警推前端 |

### 6.6 审计与访问日志勿混用

- 访问明细只在 **ClickHouse**；业务谁在何时注册/登录只在 **MySQL `audit_log`**。
- 关联键：`trace_id`（可选 `user_id`）。

## 7. 集成选型

| 类别 | 选型 |
|------|------|
| BFF 出站 | **Spring HTTP Interface** + **WebClient** + `X-Trace-Id` 过滤器 |
| DB 迁移 | **Flyway**（各服务 `db/migration`） |
| 服务发现 | `application.yml` 固定 URL（MVP） |
| 短信 | 腾讯云 SMS SDK |
| 邮件 | Spring Mail + SMTP |

## 8. 未决（选修）

- `config_json` 全量 Jasypt 加密 → MVP 密钥放环境变量
- springdoc-openapi 运行时文档 → 课后补
- logservice §2.4 WebSocket 监控推送 → 二期
- 审计写入 Kafka 主题（logservice 提及可选）→ 课程仍以 MySQL `audit_log` 为准
- logservice §5.3 高级能力（动态 DEBUG、多语言 SDK）→ 远期
