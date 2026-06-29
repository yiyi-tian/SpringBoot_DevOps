# SpringBoot DevOps 统一接口文档

> 整理自 `userservice.md`、`msgservice.md`、`logservice.md`。  
> 架构：**TopBiz（8080）对外 BFF** → User（8081）/ Message（8082）/ Log（8083）**仅内部**。

## 1. 架构说明

### 1.1 服务职责


| 服务                  | 端口   | 路径前缀        | 职责                                                |
| ------------------- | ---- | ----------- | ------------------------------------------------- |
| **topbiz**          | 8080 | `/api/v1`   | **唯一对外入口（BFF）**：鉴权与会话、参数校验、跨服务编排、对外协议适配           |
| **user-service**    | 8081 | `/internal` | 用户、分组、权限 RBAC（不对外暴露 HTTP）                         |
| **message-service** | 8082 | `/internal` | 模板、变量、载体、发送、调度（不对外暴露 `/api/v1`）                   |
| **log-service**     | 8083 | `/internal` | 审计库（MySQL）、ClickHouse 查询/指标/导出；**本服务也需部署访问日志拦截器** |


### 1.2 微服务硬约束

1. **客户端 / 前端只访问 TopBiz**（`http://localhost:8080/api/v1/`*）。
2. **禁止直连** User / Message / Log（8081–8083）；Docker Compose 与 K8s 均**不对外映射**内部端口，仅 topbiz（8080 / Ingress）可达公网。
3. **内部调用鉴权**：topbiz 调用 `/internal/*` 时携带 Header `X-Internal-Token`（docker/k8s 环境必配）；dev IDE 未配置 token 时可跳过，便于本地联调。
4. **会话仅存在于 TopBiz**：登录后由 TopBiz + **Apache Shiro** 管理（md 要求，不用 JWT）；Session 持久化在 **Redis**（Spring Session，`JSESSIONID` Cookie）；内部调用**不**携带浏览器 Session。
5. **领域数据归属**：用户数据只在 user-service；消息模板/载体只在 message-service；日志只在 log-service。TopBiz **不落库、不实现领域逻辑**，只做编排。表结构见 [DATA_MODEL.md](./DATA_MODEL.md)。
6. **链路追踪**：入口在 TopBiz 生成或接收 `trace_id`，经 Header `X-Trace-Id` 透传到四服务 **AccessLogInterceptor** 及 **HTTP Interface（WebClient）** 出站（见 [logservice.md](../../logservice.md) §4–§5、[ADR.md](../ADR.md) §6、[§1.6](#16-日志架构拦截器--双存储)）。
7. **跨服务禁止复制用户表**：message-service **不**维护完整 `user` 档案表，仅保存 `initiator_user_id`、`receiver` 等逻辑 ID（见 DATA_MODEL §3）。

部署细节见 [DEVELOPMENT.md §10](./DEVELOPMENT.md#10-docker-与-kubernetes-部署) 与 [infra/k8s/README.md](../infra/k8s/README.md)。

### 1.3 Message 内部路径映射

飞书 md 中 Message 曾使用 `/api/internal/v1/`*，本仓库文档统一为 `/internal/*`：


| md 原路径                                 | 文档统一路径                               |
| -------------------------------------- | ------------------------------------ |
| `/api/internal/v1/message-templates`   | `/internal/message-templates`        |
| `/api/internal/v1/sending-records`     | `/internal/sending-records`          |
| `/api/internal/v1/messages/instant`    | `/internal/messages/instant`         |
| `/api/internal/v1/messages/scheduleda` | `/internal/messages/scheduled`（纠正拼写） |
| `/api/internal/v1/scheduler/trigger`   | `/internal/scheduler/trigger`        |
| `/internal/variables/storage`          | `/internal/variables`                |


msgservice 载体表写「Msg 模块处理」`/api/v1/msg/carriers`：对外仍只出现在 **TopBiz**；实现落在 message-service 的 `/internal/msg/carriers`*。

### 1.4 典型编排示例

**注册（单一凭证+密码）**

```
Client  →  topbiz POST /api/v1/register          （各服务 AccessLogInterceptor 自动记访问日志 → JSON → Vector → ClickHouse）
         →  user-service POST /internal/user/register
         →  message-service POST /internal/messages/instant（欢迎站内信，templateId=1）
         →  log-service POST /internal/log/record   （仅业务审计：operation=USER_REGISTER）
```

**创建消息模板**（访问日志同样由拦截器采集，不写 record）

```
Client  →  topbiz POST /api/v1/templates
         →  message-service POST /internal/message-templates
```

**创建模板变量（md 多步编排）**

```
Client  →  topbiz POST /api/v1/variables
         →  message-service POST /internal/variables
         →  message-service POST /internal/message/send
```

### 1.5 课程必做范围（MVP）

以下为本课程验收必做；**§2.1、§2.10、§2.11** 与之对应。§2.3–§2.7 管理员 RBAC、§2.8–§2.9 模板变量、定时发送等为**选修/扩展**。

#### 用户注册 / 登录（必做 6 场景）


| 场景 | 对外 API | 请求体要点 |
|------|----------|-----------|
| 注册：发验证码 | `POST /api/v1/register` | 仅 `{ "email": "..." }` 或 `{ "phone": "..." }`（手机 501） |
| 注册：邮箱+验证码 | 同上 | `{ "email", "code", "password" }`（须先发码；仅 `{ "email", "code" }` 返回 400） |
| 注册：邮箱+密码 | 同上 | `{ "email", "password" }`（仅格式校验，不发确认邮件） |
| 注册：手机/用户名+密码 | 同上 | `{ "phone"/"credential", "password" }` |
| 登录：发验证码 | `POST /api/v1/login` | 仅 `{ "email" }` |
| 登录：邮箱+验证码 | 同上 | `{ "email", "code" }` |
| 登录：密码 | 同上 | `{ "email"/"phone"/"credential", "password" }` |

> **对外仅 `register` 与 `login` 两个路径**；发码、验码、落库均在 TopBiz 内部分发。凭证类型由 `email`/`phone`/`credential` 字段自动推断；严格正则见 `CredentialValidator`。


> **验证码注册**：发码后须在同一次 `POST /api/v1/register` 中提交 `email`、`code`、`password`（前端可先验码再设密，最终一次提交）。  
> **验证码登录**：`POST /api/v1/login` 提交 `{ "email", "code" }` 即可，无需设密。

#### 消息通告（必做 3 载体）


| 载体          | channelType   | provider（载体配置） | 主要 API                                                                                |
| ----------- | ------------- | -------------- | ------------------------------------------------------------------------------------- |
| 站内信（本地系统）   | `IN_APP`      | 无第三方           | `POST /api/v1/send/instant`                                                           |
| 手机短信（腾讯云额度） | `TENCENT_SMS` | `TENCENT_SMS`  | `POST /api/v1/msg/carriers` + `POST .../send/instant` + `POST .../carriers/{id}/test` |
| 邮件          | `EMAIL`       | `SMTP` 或邮件服务商  | 同上 + 验证码走 `.../email_code/send`、`.../phone_code/send`                                 |


数据表与枚举见 [DATA_MODEL.md](./DATA_MODEL.md) §2–§3。

### 1.6 日志架构（拦截器 + 双存储）

依据 [logservice.md](../../logservice.md)，与业务接口 **分离**：


| 类型               | 产生方式                                             | 存储                      | 是否每次 HTTP 都调 `record` |
| ---------------- | ------------------------------------------------ | ----------------------- | --------------------- |
| **访问日志（Access）** | **四个服务**均部署 `AccessLogInterceptor`，请求结束写 JSON 文件 | Vector → **ClickHouse** | **否**                 |
| **审计日志（Audit）**  | 注册/登录/登出、管理写操作等；TopBiz 编排成功后调 `record`           | MySQL `**audit_log`**   | 仅关键业务事件               |


**各服务 `service_name`（访问日志统一枚举）**：`topbiz` | `user` | `message` | `log`

**访问日志 JSON 字段**（与 logservice §4 一致，snake_case）：

```json
{
  "trace_id": "uuid-xxx",
  "service_name": "topbiz",
  "client_ip": "192.168.1.1",
  "method": "POST",
  "uri": "/api/v1/login",
  "cost_ms": 45,
  "http_status": 200,
  "biz_code": "0",
  "timestamp": 1690000000000,
  "req_params": "{\"credential\":\"...\"}",
  "res_body": "{\"code\":0}"
}
```

**审计 `POST /internal/log/record` 请求体（示例）**：`trace_id`、`user_id`、`operation`（如 `USER_LOGIN`）、`success`、`target_id` 等，见 [DATA_MODEL.md](./DATA_MODEL.md) §4。

`**req_params` / `res_body` 记录策略**（[ADR.md](../ADR.md) §6.2）：默认脱敏 + 长度截断；可配置 `body-on-error-only`（仅 `http_status>=400` 或 `biz_code!=0` 时记全量）。`cost_ms` > 3000ms 时应用日志 **WARN**（logservice §5.1 慢请求）。

**JSONL 写盘路径**（与 [README.md](../README.md) 目录说明一致）：


| 场景                           | 路径                                                                           |
| ---------------------------- | ---------------------------------------------------------------------------- |
| IDE 本地 `mvn spring-boot:run` | `shared-logs/access/{serviceName}/access-{date}.jsonl`                       |
| Docker 全栈部署                  | 容器内 `logs/access/{serviceName}/access-{date}.jsonl`（compose 卷 `access_logs`） |
| Vector 采集                    | `/var/log/access/*/*.jsonl`（local compose 将 `shared-logs/access` 挂载到该路径）     |


> **本地开发环境**：MySQL/Redis 连云端；ClickHouse/Vector 本机 Docker（见 [变更汇总.md](./变更汇总.md)）。

> user-service / message-service **不**应对外 Client 暴露，但仍须拦截 **TopBiz 发来的 internal 请求**并记 `uri` 为 internal 路径（如 `/internal/user/login`）。

---

## 2. TopBiz 对外接口（客户端调用）

**Base URL**：`http://localhost:8080`  
**说明**：下列「编排下游」为规格目标；TopBiz **转发/编排**，业务实现在对应微服务。

**表格列说明**

- **必做**：`是` = 课程 MVP（§1.5）；`选修` = 扩展能力。
- **鉴权**：`无` = 注册/登录前；`Shiro` = 需 TopBiz 会话；`Shiro + admin` = 需管理员角色（`roles[admin]`）。
- **规格状态**：`planned` = 仅文档；`partial` = 部分实现；`done` = 与当前代码一致（极少）。

> **访问日志**：凡经 topbiz / user / message / log 的 HTTP，均由该服务 **AccessLogInterceptor** 写入本地 JSON，再经 Vector 入库 ClickHouse；**编排链中不必也不应逐条列出**。  
> **审计日志**：仅下表标注 `record` 的接口在业务成功后由 TopBiz 调用 `POST /internal/log/record`。

### 2.1 注册 / 登录 / 登出（必做 + 账户管理）


| 必做  | 方法   | 路径                            | 说明                | 鉴权    | 请求体要点                                      | 编排下游（完整路径）                                                                                      | 规格状态    |
| --- | ---- | ----------------------------- | ----------------- | ----- | ------------------------------------------ | ----------------------------------------------------------------------------------------------- | ------- |
| 是   | POST | `/api/v1/register` | 注册（发码/验证码+密码/密码，统一入口） | 无 | 见 §1.5 请求体矩阵 | TopBiz 内部分发 → message internal 发码/验码、user register | done |
| 是   | POST | `/api/v1/login` | 登录（发码/验证码/密码，统一入口） | 无 | 见 §1.5 | TopBiz → message/user；PHONE 发码/验证码 501 | done |
| 选修  | POST | `/api/v1/deregister`          | 注销账号              | Shiro | 会话 userId                                  | `user-service POST /internal/user/{userId}/deregister`；可选 `record`（`USER_DEREGISTER`）           | planned |
| 选修  | POST | `/api/v1/logout`              | 登出                | Shiro | 会话 userId                                  | `user-service POST /internal/user/{userId}/logout`；可选 `record`（`USER_LOGOUT`）                   | planned |


飞书/微信扫码注册登录为**选修**；userservice.md 1.1.3.3 手机登录接口表错位，**以本表为准**（见 [GAPS.md](./GAPS.md) §4）。

### 2.2 用户查询与运维日志（选修）


| 方法   | 路径                           | 说明                                     | 鉴权            | 编排下游                                                   | 规格状态    |
| ---- | ---------------------------- | -------------------------------------- | ------------- | ------------------------------------------------------ | ------- |
| GET  | `/api/v1/permissions`        | 查看当前用户权限                               | Shiro         | `user-service GET /internal/user/{userId}/permissions` | planned |
| GET  | `/api/v1/groups`             | 查看当前用户分组                               | Shiro         | `user-service GET /internal/user/{userId}/groups`      | planned |
| GET  | `/api/v1/log`                | **用户审计历史**（MySQL `audit_log`）          | Shiro + admin | `log-service GET /internal/log/{userId}/query`；可选 query `userId` 查指定用户 | done    |
| GET  | `/api/v1/log/ops/query`      | **运维原始日志查询**（ClickHouse）               | Shiro + admin | `log-service GET /internal/log/ops/query`              | done    |
| POST | `/api/v1/log/ops/query`      | **运维日志复杂查询**（JSON 条件）                  | Shiro + admin | `log-service POST /internal/log/ops/query`             | done    |
| GET  | `/api/v1/log/metrics`        | **日志指标**（QPS/P99/错误率等，logservice §1.2） | Shiro + admin | `log-service GET /internal/log/metrics`                | done    |
| POST | `/api/v1/log/ops/export`     | 导出日志 CSV/JSON/TXT                      | Shiro + admin | `log-service POST /internal/log/ops/export`            | done    |
| GET  | `/api/v1/log/metrics/config` | 读取指标告警阈值                               | Shiro + admin | `log-service GET /internal/log/metrics/config`         | done    |
| PUT  | `/api/v1/log/metrics/config` | 更新指标告警阈值                               | Shiro + admin | `log-service PUT /internal/log/metrics/config`         | done    |


> **勿混淆**：`GET /api/v1/log` = 业务**审计**（谁注册/登录/删用户）；`GET /api/v1/log/ops/query` = **访问/运维日志**（拦截器 + ClickHouse，按 trace_id/service 检索）。  
> 查询参数见 §5（`service_name`、`start_time`、`end_time`、`trace_id`、`keyword`、`level` 等）。

### 2.3 管理员 - 用户（选修）

TopBiz 鉴权后转发；User 服务不对外。


| 方法     | 路径                    | 说明     | 鉴权    | 编排下游                                                                                                                         | 规格状态    |
| ------ | --------------------- | ------ | ----- | ---------------------------------------------------------------------------------------------------------------------------- | ------- |
| POST   | `/api/v1/admin/users` | 新增用户   | Shiro | `user-service POST /internal/user/create` → `message-service POST /internal/messages/instant`（欢迎站内信）；成功 → `record`（`ADMIN_USER_CREATE`） | partial |
| DELETE | `/api/v1/admin/users` | 删除用户   | Shiro | `user-service DELETE /internal/user/delete`；成功 → `record`（`ADMIN_USER_DELETE`）                                               | planned |
| PATCH  | `/api/v1/admin/users` | 修改用户   | Shiro | `user-service PATCH /internal/user/update`；成功 → `record`（`ADMIN_USER_UPDATE`）                                                | planned |
| GET    | `/api/v1/admin/users` | 查询用户列表 | Shiro | `user-service GET /internal/user/search`                                                                                     | planned |


### 2.4 管理员 - 分组（选修）


| 方法     | 路径                     | 说明   | 鉴权    | 编排下游                                         | 规格状态    |
| ------ | ---------------------- | ---- | ----- | -------------------------------------------- | ------- |
| POST   | `/api/v1/admin/groups` | 创建分组 | Shiro | `user-service POST /internal/group/create`   | planned |
| DELETE | `/api/v1/admin/groups` | 删除分组 | Shiro | `user-service DELETE /internal/group/delete` | planned |
| PATCH  | `/api/v1/admin/groups` | 修改分组 | Shiro | `user-service PATCH /internal/group/update`  | planned |
| GET    | `/api/v1/admin/groups` | 查询分组 | Shiro | `user-service GET /internal/group/search`    | planned |


### 2.5 管理员 - 用户组成员（选修）


| 方法     | 路径                          | 说明     | 鉴权    | 编排下游                                              | 规格状态    |
| ------ | --------------------------- | ------ | ----- | ------------------------------------------------- | ------- |
| POST   | `/api/v1/admin/group-users` | 添加用户到组 | Shiro | `user-service POST /internal/group-user/create`   | planned |
| DELETE | `/api/v1/admin/group-users` | 从组移除用户 | Shiro | `user-service DELETE /internal/group-user/delete` | planned |
| GET    | `/api/v1/admin/group-users` | 查询组成员  | Shiro | `user-service GET /internal/group-user/search`    | planned |


### 2.6 管理员 - 权限（选修）


| 方法     | 路径                          | 说明   | 鉴权    | 编排下游                                              | 规格状态    |
| ------ | --------------------------- | ---- | ----- | ------------------------------------------------- | ------- |
| POST   | `/api/v1/admin/permissions` | 创建权限 | Shiro | `user-service POST /internal/permission/create`   | planned |
| DELETE | `/api/v1/admin/permissions` | 删除权限 | Shiro | `user-service DELETE /internal/permission/delete` | planned |
| PATCH  | `/api/v1/admin/permissions` | 修改权限 | Shiro | `user-service PATCH /internal/permission/update`  | planned |
| GET    | `/api/v1/admin/permissions` | 查询权限 | Shiro | `user-service GET /internal/permission/search`    | planned |


### 2.7 管理员 - 分组权限（选修）


| 方法     | 路径                                | 说明     | 鉴权    | 编排下游                                                    | 规格状态    |
| ------ | --------------------------------- | ------ | ----- | ------------------------------------------------------- | ------- |
| POST   | `/api/v1/admin/group-permissions` | 创建分组权限 | Shiro | `user-service POST /internal/group-permission/create`   | planned |
| DELETE | `/api/v1/admin/group-permissions` | 删除分组权限 | Shiro | `user-service DELETE /internal/group-permission/delete` | planned |
| PATCH  | `/api/v1/admin/group-permissions` | 修改分组权限 | Shiro | `user-service PATCH /internal/group-permission/update`  | planned |
| GET    | `/api/v1/admin/group-permissions` | 查询分组权限 | Shiro | `user-service GET /internal/group-permission/search`    | planned |


### 2.8 消息 - 模板（选修，TopBiz 转发 → message-service）


| 方法   | 路径                              | 说明     | 鉴权    | 编排下游                                                                                 | 规格状态    |
| ---- | ------------------------------- | ------ | ----- | ------------------------------------------------------------------------------------ | ------- |
| POST | `/api/v1/templates`             | 创建消息模板 | Shiro | `message-service POST /internal/message-templates`（默认 status=DRAFT；instant 前须改为 ACTIVE） | done |
| GET  | `/api/v1/templates`             | 查询模板列表 | Shiro | `message-service GET /internal/message-templates`                                    | done |
| PUT  | `/api/v1/templates/{id}/status` | 模板状态变更 | Shiro | `message-service PUT /internal/message-templates`（body 含 `id`、目标状态；TopBiz 做 path 映射） | done |


### 2.9 消息 - 模板变量（选修，TopBiz 转发 → message-service）


| 方法     | 路径                               | 说明     | 鉴权    | 编排下游                                                                                                                               | 规格状态    |
| ------ | -------------------------------- | ------ | ----- | ---------------------------------------------------------------------------------------------------------------------------------- | ------- |
| GET    | `/api/v1/variables/schema`       | 变量定义规则 | Shiro | `message-service GET /internal/variables/schema`（静态规则）                               | done |
| POST   | `/api/v1/variables`              | 创建模板变量 | Shiro | `message-service POST /internal/variables`（当前返回 501）                                 | partial |
| GET    | `/api/v1/variables/{variableId}` | 查询单个变量 | Shiro | `message-service GET /internal/variables/{variableId}`（501）                           | partial |
| PUT    | `/api/v1/variables/{variableId}` | 修改变量   | Shiro | `message-service PUT /internal/variables/{variableId}`（501）                            | partial |
| DELETE | `/api/v1/variables/{variableId}` | 删除变量   | Shiro | `message-service DELETE /internal/variables/{variableId}`（501）                         | partial |


### 2.10 消息 - 载体（必做，TopBiz 转发 → message-service）

课程必做需至少配置三种载体（可先 POST 创建，再 instant 发送）：


| channel_type  | provider      | config_json 要点（文档级）                                                    |
| ------------- | ------------- | ---------------------------------------------------------------------- |
| `IN_APP`      | —             | 无第三方密钥                                                                 |
| `TENCENT_SMS` | `TENCENT_SMS` | `secretId`, `secretKey`, `sdkAppId`, `signName`, `templateId`（腾讯云短信额度） |
| `EMAIL`       | `SMTP`        | `host`, `port`, `username`, `password`, `from`                         |



| 必做  | 方法     | 路径                               | 说明                    | 鉴权    | 编排下游                                                      | 规格状态    |
| --- | ------ | -------------------------------- | --------------------- | ----- | --------------------------------------------------------- | ------- |
| 是   | GET    | `/api/v1/msg/carriers`           | 载体列表；可筛 `channelType` | Shiro | `message-service GET /internal/msg/carriers?channelType=` | done |
| 是   | GET    | `/api/v1/msg/carriers/{id}`      | 载体详情                  | Shiro | `message-service GET /internal/msg/carriers/{id}`         | done |
| 是   | POST   | `/api/v1/msg/carriers`           | 新增载体                  | Shiro | `message-service POST /internal/msg/carriers`             | done |
| 是   | PUT    | `/api/v1/msg/carriers/{id}`      | 修改载体                  | Shiro | `message-service PUT /internal/msg/carriers/{id}`         | done |
| 是   | DELETE | `/api/v1/msg/carriers/{id}`      | 删除载体                  | Shiro | `message-service DELETE /internal/msg/carriers/{id}`      | done |
| 是   | POST   | `/api/v1/msg/carriers/{id}/test` | 连通性测试（**当前仅 EMAIL**） | Shiro | `message-service POST /internal/msg/carriers/{id}/test`；body `{"testTo":"收件邮箱"}` | partial |


### 2.11 消息 - 发送与记录（必做 instant + 选修 scheduled）

#### `POST /api/v1/send/instant` 请求体（必做）


| 字段            | 说明                                        |
| ------------- | ----------------------------------------- |
| `channelType` | `IN_APP` \| `TENCENT_SMS` \| `EMAIL`（必做三选一；`FEISHU`/`WECHAT` 返回 501） |
| `templateId`  | 消息模板 ID（**必填**；须为 status=ACTIVE 的模板） |
| `receiver`    | 收件人：`userId`（站内信）/ 手机号 / 邮箱               |
| `variables`   | 模板变量键值对（`${varName}` 占位符替换）                 |
| `carrierId`   | 可选；指定载体；不填则按 channelType 选默认启用载体          |

TopBiz 自动注入 `initiator_user_id`（当前登录用户 ID，snake_case），写入 `msg_message` 流水。


**三种必发示例（逻辑）**


| channelType   | receiver 示例        | 说明                |
| ------------- | ------------------ | ----------------- |
| `IN_APP`      | `userId=1001`      | 写入站内消息表，无第三方      |
| `TENCENT_SMS` | `13800000000`      | **当前返回 501「短信未配置」** |
| `EMAIL`       | `user@example.com` | 经 SMTP 发业务邮件      |


验证码类邮件/短信走 §2.1 的 `register/email_code`、`login/email_code` 等，**不经过** instant（或 instant 仅作统一底层实现时需在实现说明中注明）。


| 必做  | 方法     | 路径                             | 说明        | 鉴权    | 编排下游                                                | 规格状态    |
| --- | ------ | ------------------------------ | --------- | ----- | --------------------------------------------------- | ------- |
| 是   | POST   | `/api/v1/send/instant`         | 即时发送（IN_APP/EMAIL 可用；SMS 501） | Shiro | `message-service POST /internal/messages/instant`   | partial |
| 选修  | GET    | `/api/v1/sending-records`      | 查看发送记录    | Shiro | `message-service GET /internal/sending-records`     | done |
| 选修  | DELETE | `/api/v1/sending-records/{id}` | 删除发送记录    | Shiro | `message-service DELETE /internal/sending-records`（body `{id}`） | done |
| 选修  | POST   | `/api/v1/send/scheduled`       | 定时发送      | Shiro | `message-service POST /internal/messages/scheduled`（501 未实现） | partial |


---

## 3. User Service 内部接口

**Base URL**：`http://localhost:8081`  
**调用方**：仅 **TopBiz**（管理员/用户类对外接口转发）。  
**路径风格**：md 约定为动作型路径（如 `/internal/user/create`），非 `/internal/users/{id}` 资源化。

**访问日志**：须注册 `AccessLogInterceptor`，`service_name=user`，`uri` 记实际 internal 路径；日志文件由 Vector 采集至 ClickHouse（见 §1.6）。**不**调用 `POST /internal/log/record`。


| 方法     | 路径                                    | 说明      | 调用方    | md 章节    | 规格状态    |
| ------ | ------------------------------------- | ------- | ------ | -------- | ------- |
| POST   | `/internal/user/register`             | 创建用户    | TopBiz | 1.1.1    | partial |
| POST   | `/internal/user/login`                | 登录校验    | TopBiz | 1.1.3    | planned |
| POST   | `/internal/user/{userId}/deregister`  | 注销      | TopBiz | 1.1.2    | planned |
| POST   | `/internal/user/{userId}/logout`      | 登出      | TopBiz | 1.1.4    | planned |
| GET    | `/internal/user/{userId}/permissions` | 用户权限    | TopBiz | 1.1.5    | planned |
| GET    | `/internal/user/{userId}/groups`      | 用户分组    | TopBiz | 1.1.6    | planned |
| POST   | `/internal/user/create`               | 管理员创建用户 | TopBiz | 1.1.12.1 | planned |
| DELETE | `/internal/user/delete`               | 管理员删除用户 | TopBiz | 1.1.12.2 | planned |
| PATCH  | `/internal/user/update`               | 管理员更新用户 | TopBiz | 1.1.12.3 | planned |
| GET    | `/internal/user/search`               | 管理员查询用户 | TopBiz | 1.1.12.4 | planned |
| POST   | `/internal/group/create`              | 创建分组    | TopBiz | 1.1.13.1 | planned |
| DELETE | `/internal/group/delete`              | 删除分组    | TopBiz | 1.1.13.2 | planned |
| PATCH  | `/internal/group/update`              | 修改分组    | TopBiz | 1.1.13.3 | planned |
| GET    | `/internal/group/search`              | 查询分组    | TopBiz | 1.1.13.4 | planned |
| POST   | `/internal/group-user/create`         | 添加组成员   | TopBiz | 1.1.14.1 | planned |
| DELETE | `/internal/group-user/delete`         | 移除组成员   | TopBiz | 1.1.14.2 | planned |
| GET    | `/internal/group-user/search`         | 查询组成员   | TopBiz | 1.1.14.3 | planned |
| POST   | `/internal/permission/create`         | 创建权限    | TopBiz | 1.1.15.1 | planned |
| DELETE | `/internal/permission/delete`         | 删除权限    | TopBiz | 1.1.15.2 | planned |
| PATCH  | `/internal/permission/update`         | 修改权限    | TopBiz | 1.1.15.3 | planned |
| GET    | `/internal/permission/search`         | 查询权限    | TopBiz | 1.1.15.4 | planned |
| POST   | `/internal/group-permission/create`   | 创建组权限   | TopBiz | 1.1.16.1 | planned |
| DELETE | `/internal/group-permission/delete`   | 删除组权限   | TopBiz | 1.1.16.2 | planned |
| PATCH  | `/internal/group-permission/update`   | 修改组权限   | TopBiz | 1.1.16.3 | planned |
| GET    | `/internal/group-permission/search`   | 查询组权限   | TopBiz | 1.1.16.4 | planned |


> **数据模型**：`user_permission` 表见 [DATA_MODEL.md](./DATA_MODEL.md) §2；userservice 中「用户直接权限」独立 CRUD 接口尚未在 md 定稿（§7），当前通过 `GET /internal/user/{userId}/permissions` 聚合查询。

---

## 4. Message Service 内部接口

**Base URL**：`http://localhost:8082`  
**调用方**：**TopBiz**（消息域对外 API 转发）；**自驱任务**（仅 scheduler，见末行）。

**访问日志**：须注册 `AccessLogInterceptor`，`service_name=message`；自驱 `scheduler/trigger` 同样经过拦截器（`uri=/internal/scheduler/trigger`）。


| 方法     | 路径                                  | 说明                                                  | 调用方                         | md 原路径（已映射）                                | 规格状态    |
| ------ | ----------------------------------- | --------------------------------------------------- | --------------------------- | ------------------------------------------ | ------- |
| POST   | `/internal/message-templates`       | 创建模板                                                | TopBiz                      | `/api/internal/v1/message-templates`       | done |
| GET    | `/internal/message-templates`       | 查询模板                                                | TopBiz                      | 同上                                         | done |
| PUT    | `/internal/message-templates`       | 更新模板/状态                                             | TopBiz                      | 同上；对外 `PUT .../templates/{id}/status` 映射到此 | done |
| POST   | `/internal/messages/instant`        | 即时发送；body 含 `channelType`（IN_APP/TENCENT_SMS/EMAIL） | TopBiz                      | `/api/internal/v1/messages/instant`        | partial |
| POST   | `/internal/message/email_code/send` | 邮箱验证码                                               | TopBiz                      | 保留                                         | done |
| POST   | `/internal/message/phone_code/send` | 手机验证码                                               | TopBiz                      | 保留（dev 控制台）                                 | partial |
| POST   | `/internal/message/verify`          | 校验验证码（body: credentialType/target/scene/code）       | TopBiz                      | AuthService 编排                              | done |
| GET    | `/internal/variables/schema`        | 变量规则（静态）                                            | TopBiz                      | 对外 `GET /api/v1/variables/schema` 转发       | done |
| POST   | `/internal/variables`               | 持久化变量                                               | TopBiz                      | `/internal/variables/storage`              | partial |
| GET    | `/internal/variables/{variableId}`  | 查询变量                                                | TopBiz                      | `/internal/storage/variables/{id}`         | partial |
| PUT    | `/internal/variables/{variableId}`  | 修改变量                                                | TopBiz                      | 同上                                         | partial |
| DELETE | `/internal/variables/{variableId}`  | 删除变量                                                | TopBiz                      | 同上                                         | partial |
| GET    | `/internal/msg/carriers`            | 载体列表                                                | TopBiz                      | 对外 `/api/v1/msg/carriers`                  | done |
| GET    | `/internal/msg/carriers/{id}`       | 载体详情                                                | TopBiz                      | 同上                                         | done |
| POST   | `/internal/msg/carriers`            | 新增载体                                                | TopBiz                      | 同上                                         | done |
| PUT    | `/internal/msg/carriers/{id}`       | 修改载体                                                | TopBiz                      | 同上                                         | done |
| DELETE | `/internal/msg/carriers/{id}`       | 删除载体                                                | TopBiz                      | 同上                                         | done |
| POST   | `/internal/msg/carriers/{id}/test`  | 连通性测试（EMAIL；body `testTo`）                          | TopBiz                      | 同上                                         | partial |
| GET    | `/internal/sending-records`         | 发送记录                                                | TopBiz                      | `/api/internal/v1/sending-records`         | done |
| DELETE | `/internal/sending-records`         | 删除记录（body `{id}`）                                     | TopBiz                      | 同上                                         | done |
| POST   | `/internal/messages/scheduled`      | 定时发送（501）                                            | TopBiz                      | `/api/internal/v1/messages/scheduled`      | partial |
| POST   | `/internal/scheduler/trigger`       | 调度触发（501）                                            | **自驱（message-service 调度器）** | `/api/internal/v1/scheduler/trigger`       | partial |


> `POST /internal/scheduler/trigger`：**无** TopBiz 对外 API；由 message-service 内部定时任务调用，不面向 Client。

---

## 5. Log Service 内部接口

**Base URL**：`http://localhost:8083`  
**职责拆分**（[logservice.md](../../logservice.md)）：


| 能力              | 机制                                                          | 存储                    | MVP |
| --------------- | ----------------------------------------------------------- | --------------------- | --- |
| 访问/运维日志         | 四服务 **AccessLogInterceptor** + **Vector**                   | **ClickHouse**        | 是   |
| 业务审计            | TopBiz 编排 `**POST /internal/log/record`**                   | MySQL `**audit_log**` | 是   |
| 指标查询            | `**GET /internal/log/metrics**`（查询时聚合，§3.7）                 | ClickHouse            | 是   |
| 运维检索/导出         | `ops/query`、`ops/export`                                    | ClickHouse            | 是   |
| 指标阈值配置          | `**GET/PUT /internal/log/metrics/config**`（logservice §1.3） | MySQL 配置表             | 是   |
| 监控 WebSocket 推送 | log-service 自驱（logservice §2.4）                             | —                     | 选修  |
| 定时预聚合           | `@Scheduled` 写 `metrics_aggregate`（§2.3）                    | ClickHouse            | 是   |


**访问日志**：log-service 自身亦部署拦截器，`service_name=log`。

`**record` 调用方**：**仅 TopBiz**（业务成功后写入审计）；user-service / message-service **不**直调 `record`（与 BFF 边界一致）。

### 5.1 审计写入


| 方法   | 路径                     | 说明     | 调用方          | 规格状态 |
| ---- | ---------------------- | ------ | ------------ | ---- |
| POST | `/internal/log/record` | 业务审计写入 | **仅 TopBiz** | done |


**请求体（JSON，snake_case 推荐）**：


| 字段        | 类型      | 说明                                                 |
| --------- | ------- | -------------------------------------------------- |
| trace_id  | string  | 与 `X-Trace-Id` 一致                                  |
| user_id   | long    | 操作人；匿名注册前可为 null                                   |
| operation | string  | 如 `USER_REGISTER`、`USER_LOGIN`、`ADMIN_USER_DELETE` |
| success   | boolean | 业务是否成功                                             |
| target_id | string  | 可选，被操作对象 ID                                        |
| detail    | string  | 可选，脱敏摘要                                            |


### 5.2 用户审计查询（MySQL）


| 方法  | 路径                             | 说明       | 调用方    | 对外代理              | 规格状态 |
| --- | ------------------------------ | -------- | ------ | ----------------- | ---- |
| GET | `/internal/log/{userId}/query` | 按用户查审计列表 | TopBiz | `GET /api/v1/log` | done |


查询参数：`page`、`size`、`operation`（可选）、`start_time`、`end_time`。

### 5.3 运维日志查询（ClickHouse）


| 方法   | 路径                        | 说明                  | 调用方    | 对外代理                         | 规格状态 |
| ---- | ------------------------- | ------------------- | ------ | ---------------------------- | ---- |
| GET  | `/internal/log/ops/query` | 原始访问日志检索（Query 参数）  | TopBiz | `GET /api/v1/log/ops/query`  | done |
| POST | `/internal/log/ops/query` | 原始访问日志检索（JSON 复杂条件） | TopBiz | `POST /api/v1/log/ops/query` | done |


**时间范围**（优先级：`start_time`/`end_time` > `time_range` > 默认 **24h**）：


| 参数                        | 说明                          |
| ------------------------- | --------------------------- |
| `start_time` / `end_time` | 毫秒时间戳或 ISO-8601             |
| `time_range`              | `1h` / `24h` / `7d` / `30d` |


**筛选参数**（GET 与 POST `filters` 内字段相同）：


| 参数                                                    | 说明                                      |
| ----------------------------------------------------- | --------------------------------------- |
| `service_name` / `service_names`                      | 单值或列表                                   |
| `trace_id` / `trace_ids`                              | 链路 ID                                   |
| `api` / `uri_prefix`                                  | uri 前缀                                  |
| `uri` / `uris`                                        | 精确 uri                                  |
| `method` / `methods`                                  | HTTP 方法                                 |
| `level` / `levels`                                    | 日志级别                                    |
| `client_ip` / `client_ips`                            | 客户端 IP                                  |
| `http_status` / `http_status_min` / `http_status_max` | 状态码                                     |
| `biz_code` / `biz_codes`                              | 业务 code                                 |
| `cost_ms_min` / `cost_ms_max`                         | 耗时范围                                    |
| `slow_only`                                           | `true` 时仅慢请求（阈值见 ADR §6.2）              |
| `has_error`                                           | `true` 时仅错误请求                           |
| `keyword`                                             | uri / req_params / res_body 模糊匹配        |
| `sort_by`                                             | `timestamp` / `cost_ms` / `http_status` |
| `sort_order`                                          | `asc` / `desc`                          |
| `page` / `size`                                       | 分页                                      |


**POST 请求体示例**：

```json
{
  "time_range": "7d",
  "filters": {
    "service_names": ["log"],
    "has_error": true,
    "api": "/internal/log"
  },
  "sort": { "field": "cost_ms", "order": "desc" },
  "page": 1,
  "size": 20
}
```

### 5.4 日志指标（logservice §1.2、§3）


| 方法  | 路径                      | 说明                      | 调用方    | 对外代理                      | 规格状态 |
| --- | ----------------------- | ----------------------- | ------ | ------------------------- | ---- |
| GET | `/internal/log/metrics` | 查询时 ClickHouse 聚合（§3.7） | TopBiz | `GET /api/v1/log/metrics` | done |


查询参数：


| 参数                                       | 说明                                                                 |
| ---------------------------------------- | ------------------------------------------------------------------ |
| `source`                                 | `raw`（默认，查 `access_log`）| `aggregate`（查 `metrics_aggregate`，长区间推荐） |
| `service_name`                           | 可选                                                                 |
| `api`                                    | 可选，uri 前缀                                                          |
| `start_time` / `end_time` / `time_range` | 时间范围（规则同 §5.3）                                                     |
| `metric`                                 | 见下表                                                                |
| `top_n`                                  | 排行类指标（仅 `source=raw`）                                              |
| `interval`                               | QPS 时间桶（秒）                                                         |


`source=aggregate` 支持的 `metric`：`pv`、`qps`、`api_calls`、`error_rate`、`p95`、`p99`、`success_rate`。排行类（`slowest_api`、`ip_*`）请使用 `source=raw`。

`**metric` 枚举**（与 logservice §3 对齐；**MVP 可先实现** `qps`、`pv`、`error_rate`、`p95`、`p99`、`avg`、`success_rate`）：


| 分类       | `metric` 值                                       |
| -------- | ------------------------------------------------ |
| §3.1 请求量 | `qps`、`pv`、`api_calls`、`slow_count`              |
| §3.2 错误  | `error_rate`、`error_count`、`http_5xx`、`http_4xx` |
| §3.3 性能  | `avg`、`p95`、`p99`、`max`、`slowest_api`            |
| §3.4 稳定性 | `success_rate`                                   |
| §3.5 安全  | `ip_request_topn`、`ip_error_topn`                |


> 飞书 §1.2 接口表为空；**REST 以本表为准**。详见 [ADR.md](../ADR.md) §6.5。

### 5.4.1 指标阈值配置（选修，logservice §1.3）


| 方法  | 路径                             | 说明                                    | 对外代理                             | 规格状态 |
| --- | ------------------------------ | ------------------------------------- | -------------------------------- | ---- |
| GET | `/internal/log/metrics/config` | 读取告警阈值（error_rate、p99、success_rate 等） | `GET /api/v1/log/metrics/config` | done |
| PUT | `/internal/log/metrics/config` | 更新阈值                                  | `PUT /api/v1/log/metrics/config` | done |


> 供 §2.4 WebSocket 告警判定使用；MVP 可不实现。

### 5.5 日志导出


| 方法   | 路径                         | 说明                  | 调用方    | 对外代理                          | 规格状态 |
| ---- | -------------------------- | ------------------- | ------ | ----------------------------- | ---- |
| POST | `/internal/log/ops/export` | 导出 CSV / JSON / TXT | TopBiz | `POST /api/v1/log/ops/export` | done |


请求体：`format`（`csv`|`json`|`txt`）、与 `ops/query` 相同的筛选条件。

### 5.6 非 HTTP（基础设施与自驱，logservice §2）


| 组件           | 说明                                                                                                                                                   | MVP |
| ------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------- | --- |
| **Vector**   | 采集访问 JSONL → ClickHouse（IDE：`shared-logs/access/*/*.jsonl`；Docker：`logs/access/*/*.jsonl`；Vector 读 `/var/log/access/*/*.jsonl`；**不用 Filebeat**，§5.4） | 是   |
| ClickHouse   | `access_log` 明细；TTL 90 天（§2.1）                                                                                                                       | 是   |
| 本地 JSON 清理   | 微服务日志框架按时间/容量删旧文件（§2.1）                                                                                                                              | 是   |
| 异步采集入库       | Vector：Tail + 批量/超时刷新（§2.2）                                                                                                                          | 是   |
| 定时预聚合        | 每 5 分钟写 `metrics_aggregate`（§2.3）；`MetricsAggregateScheduler`                                                                                        | 是   |
| WebSocket 监控 | 周期推送 + 阈值告警（§2.4）                                                                                                                                    | 选修  |


---

## 6. 统一约定（实现参考）

### 6.1 响应体（md 未定义，约定占位）

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

### 6.2 认证与内部调用


| 场景                 | 约定                                                                                      |
| ------------------ | --------------------------------------------------------------------------------------- |
| 对外 Client → TopBiz | **Shiro Session**（Cookie `JSESSIONID`）                                                  |
| TopBiz → internal  | 不使用浏览器 Session；**必须**透传 `X-Trace-Id`（与访问/审计字段 `trace_id` 一致）；可选 `X-Internal-Call: true` |
| 网络                 | 8081–8083 仅本机/集群内可达，不对公网开放                                                              |


### 6.3 日志与拦截器（实现清单，logservice §5）


| 服务     | 组件                                                 | 配置要点                                                              |
| ------ | -------------------------------------------------- | ----------------------------------------------------------------- |
| 四服务    | `TraceIdFilter` + `AccessLogInterceptor`           | `service_name`；脱敏；`body-on-error-only` 见 [ADR.md](../ADR.md) §6.2 |
| 四服务    | 慢请求                                                | `cost_ms`>3000 → WARN 应用日志                                        |
| common | `GlobalExceptionHandler`                           | 建议扩展 `error_type`：BIZ/SYS/RPC/DB/AUTH（§5.1）                       |
| 各服务    | `shared-logs/access/`（IDE）或 `logs/access/`（Docker） | Vector tail 目录（local compose 挂载前者到 `/var/log/access`）             |
| topbiz | HTTP Interface + WebClient                         | 出站 `X-Trace-Id`                                                   |
| topbiz | 编排成功后                                              | `POST /internal/log/record`（仅审计，非每次 HTTP）                         |


### 6.4 凭证字段：必做场景与 `user_auth` 映射


| 必做场景（§1.5）  | credentialType             | 对外参数                     | user_auth.identity_type                |
| ----------- | -------------------------- | ------------------------ | -------------------------------------- |
| 注册/登录-凭证+密码 | PHONE / EMAIL / USERNAME 等 | `credential`, `password` | 与 credentialType 一致                    |
| 注册/登录-邮箱验证码 | EMAIL                      | `email`, `code`          | EMAIL（`code` 不入库，校验 verification_code） |
| 注册/登录-手机验证码 | PHONE                      | `phone`, `code`          | PHONE                                  |



| 层级                               | 约定                                                              |
| -------------------------------- | --------------------------------------------------------------- |
| userservice.md（历史参数名）            | `token` 泛指凭证，实现时改为上表                                            |
| [DATA_MODEL.md](./DATA_MODEL.md) | `identifier` + `secret_hash` 存库                                 |
| 当前 demo                          | [README.md](../../README.md) 使用 `phone`+`password`，仅覆盖「凭证+密码」一种 |


TopBiz 将对外字段转换为 `user-service` 的 `user` + `user_auth`；验证码由 message-service 发送（`channelType=EMAIL` 或 `TENCENT_SMS`）。

### 6.5 当前代码实现对照


| 服务              | 方法                                                                                    | 路径                            | 状态                                                                                       | 备注                                                   |
| --------------- | ------------------------------------------------------------------------------------- | ----------------------------- | ---------------------------------------------------------------------------------------- | ---------------------------------------------------- |
| topbiz          | POST                                                                                  | `/api/v1/register`、`/login/`* | partial                                                                                  | JSON + HTTP Interface + Shiro + Spring Session Redis |
| user-service    | `user` + `user_auth`                                                                  | partial                       | BCrypt；6 场景 API                                                                          |                                                      |
| message-service | carriers、templates、instant（IN_APP/EMAIL）、verify、sending-records | partial | EMAIL 真发；SMS/scheduled/variables CRUD 501；欢迎信 templateId 由 topbiz `devops.messaging.welcome` 配置 |
| log-service     | record、ops/query（GET/POST）、metrics（raw/aggregate）、export、metrics/config、预聚合 Scheduler | done                          | MySQL 审计 + ClickHouse 查询/聚合                                                              |                                                      |
| 四服务             | AccessLogInterceptor                                                                  | done                          | JSONL → `shared-logs/access/{service}/`（IDE）或 `logs/access/{service}/`（Docker）；配合 Vector |                                                      |
| 基础设施            | docker-compose + SQL                                                                  | 已提供                           | MySQL/Redis/CH/Vector；见 `docs/sql/`、`infra/`                                             |                                                      |


其余接口均为 **planned**。详见 [GAPS.md](./GAPS.md)。

---

## 7. 文档缺口（详见 GAPS.md）


| 模块                                  | 状态                                   |
| ----------------------------------- | ------------------------------------ |
| 1.1.8 修改密码                          | md 表格缺失；**非 MVP**                    |
| 1.1.9 重置密码                          | md 表格缺失；**非 MVP**                    |
| 1.1.10 修改基本信息                       | md 表格缺失；**非 MVP**                    |
| 1.1.11 申请权限                         | md 表格缺失；**非 MVP**                    |
| userservice 1.1.3.3 手机登录接口表         | 飞书表错位（抄注册表）；**MVP 以 API.md §2.1 为准** |
| 用户直接权限 CRUD（userservice 2170 行后）    | 仅占位无 URL；**非 MVP**                   |
| logservice §1.1–1.4 查询/指标/导出        | 飞书表为空；REST 以 §5 为准                   |
| logservice §1.3 指标配置、§2.4 WebSocket | API §5.4.1 占位                        |
| logservice §2.3 定时预聚合               | DATA_MODEL §4.4 占位                   |
| logservice §3 全量 metric             | 代码仅部分聚合                              |
| 四服务 AccessLogInterceptor + Vector   | logservice §4–§5、ADR §6              |
| userservice 1.2 自驱                  | 调度流程，无完整 REST 表                      |
| `docs/INFRA_AND_DOCKER.md`          | **二期**（Docker 编排，依赖本 API 定稿）         |


---

## 8. 相关文件


| 文件                                                 | 状态      | 用途                     |
| -------------------------------------------------- | ------- | ---------------------- |
| [API.md](./API.md)                                 | **已维护** | 接口与编排（人读首选）            |
| [DATA_MODEL.md](./DATA_MODEL.md)                   | **已维护** | 分库 ER 与表结构优化           |
| [GAPS.md](./GAPS.md)                               | 已维护     | 缺口、笔误、与代码差异            |
| [README.md](./README.md)                           | 已维护     | 目录说明与预览方式              |
| [components-common.yaml](./components-common.yaml) | 已维护     | 公共 OpenAPI 组件片段        |
| `topbiz.openapi.yaml` 等                            | **待生成** | 机器可读 OpenAPI（勿当作已存在链接） |
| `bundled/all-services.openapi.yaml`                | **待生成** | 四服务合并视图                |
| `canonical-endpoints.json`                         | **待生成** | 端点 JSON 清单             |


