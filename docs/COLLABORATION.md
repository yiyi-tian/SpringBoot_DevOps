# SpringBoot DevOps 项目协作分工文档


## 一、当前项目状态

| 模块 | 框架完成度 | 核心实现 | 待完善 |
|------|-----------|----------|--------|
| common | ✅ 100% | Result、TraceIdFilter、AccessLogInterceptor（JSONL 写盘、脱敏、保留清理、慢请求 WARN） | - |
| topbiz | ✅ 95% | 注册/登录、Shiro 2 + Spring Session Redis、Log API 代理（admin）、HTTP Interface + WebClient（Trace 头修复）、错误码映射 | 验证码接口 |
| user-service | ✅ 85% | 注册/登录（BCrypt）、MyBatis-Plus Boot3、Entity/Mapper、getPermissions/admin 最小判定 | 管理员 CRUD、完整 RBAC |
| message-service | ✅ 60% | 即时发送骨架 | 验证码、datasource、载体、模板 |
| log-service | ✅ 95% | 审计 MySQL 持久化、ClickHouse 查询/指标/导出、阈值配置、预聚合；dev 配置模板 | WebSocket 告警 |

---

## 二、三人分工

### 👤 成员A：TopBiz + Message-Service

**负责服务**：topbiz（8080）、message-service（8082）

| 优先级 | 模块 | 任务 | 对应文件 |
|--------|------|------|----------|
| 🔴 P0 | topbiz | 实现验证码注册/登录 4 个接口 | `AuthController.java`、`AuthService.java` |
| 🟡 P1 | topbiz | 完整 RBAC 权限表（当前 admin 最小判定已实现） | `ShiroRealm.java`、user-service 权限 API |
| 🔴 P0 | message | 实现验证码发送（邮箱+手机） | `MessageService.java`（sendEmailCode/sendPhoneCode） |
| 🔴 P0 | message | 实现验证码校验 | `MessageService.java`（verifyCode） |
| 🔴 P0 | message | 创建 MySQL 表 | `msg_carrier`、`msg_task`、`msg_message` |
| 🟡 P1 | message | 实现载体管理 CRUD | `MessageService.java`（carriers 相关） |
| 🟡 P1 | message | 实现消息模板 CRUD | `MessageService.java`（templates 相关） |
| 🟡 P1 | message | 实现发送记录查询/删除 | `MessageService.java`（sending-records） |
| 🟡 P1 | topbiz | 对接管理员/消息/日志 Controller | 已有框架，联调验证 |
| 🟢 P2 | message | 接入腾讯云 SMS SDK | 真实短信发送 |
| 🟢 P2 | message | 接入 Spring Mail | 真实邮件发送 |
| 🟢 P2 | message | 实现定时发送 | `sendScheduled` + `triggerScheduler` |

**文件清单**：

```
topbiz/
├── controller/
│   ├── AuthController.java          ← 完善验证码接口
│   ├── AdminController.java         ← 联调
│   ├── MessageController.java       ← 联调
│   └── LogController.java           ← 联调
├── service/
│   ├── AuthService.java             ← 完善验证码逻辑
│   ├── AdminService.java
│   ├── MessageService.java
│   └── LogService.java
└── config/
    └── ShiroRealm.java              ← admin 判定已实现；完整 RBAC 待扩展

message-service/
├── controller/
│   └── MessageController.java       ← 联调
├── service/
│   └── MessageService.java          ← 实现所有 TODO
└── config/
    └── WebMvcConfig.java            ← 新增拦截器配置
```

---

### 👤 成员B：User-Service

**负责服务**：user-service（8081）

| 优先级 | 模块 | 任务 | 对应文件 |
|--------|------|------|----------|
| 🔴 P0 | user | 创建 MySQL 表 | `user`、`user_auth` |
| 🔴 P0 | user | 实现用户注销/登出 | `UserService.java`（deregister/logout） |
| 🔴 P0 | user | 实现用户权限查询 | `UserService.java`（getPermissions，含 admin 最小判定） |
| 🔴 P0 | user | 实现用户分组查询 | `UserService.java`（getGroups） |
| 🟡 P1 | user | 实现管理员 CRUD（用户） | `UserService.java`（createUser/deleteUser/updateUser/searchUsers） |
| 🟡 P1 | user | 实现管理员 CRUD（分组） | `UserService.java`（groups 相关） |
| 🟡 P1 | user | 实现管理员 CRUD（权限） | `UserService.java`（permissions 相关） |
| 🟡 P1 | user | 实现用户组管理 | `UserService.java`（group-users 相关） |
| 🟡 P1 | user | 实现分组权限管理 | `UserService.java`（group-permissions 相关） |
| 🟢 P2 | user | 创建 RBAC 相关表 | `group`、`permission`、`user_group`、`group_permission`、`user_permission` |
| 🟢 P2 | user | 接入 WebMvcConfig | 注册 TraceIdFilter + AccessLogInterceptor |

**文件清单**：

```
user-service/
├── controller/
│   └── UserController.java          ← 联调
├── service/
│   └── UserService.java             ← 实现所有 TODO
├── entity/
│   ├── User.java                    ← 已有
│   └── UserAuth.java                ← 已有
├── mapper/
│   ├── UserMapper.java              ← 已有
│   └── UserAuthMapper.java          ← 已有
└── config/
    └── WebMvcConfig.java            ← 新增拦截器配置
```

---

### 👤 成员C：Log-Service

**负责服务**：log-service（8083）

| 优先级 | 模块 | 任务 | 对应文件 |
|--------|------|------|----------|
| 🔴 P0 | log | 创建 MySQL 表 | `audit_log` |
| 🔴 P0 | log | 实现审计日志 MySQL 持久化 | `LogService.java`（recordAudit） |
| 🔴 P0 | log | 实现用户审计查询 | `LogService.java`（queryUserAudit） |
| 🟡 P1 | log | 搭建 ClickHouse | 创建 `access_log` 表 |
| 🟡 P1 | log | 实现运维日志查询 | `LogService.java`（queryOpsLogs） |
| 🟡 P1 | log | 实现指标查询 | `LogService.java`（queryMetrics） |
| 🟡 P1 | log | 实现日志导出 | `LogService.java`（exportLogs） |
| 🟢 P2 | log | 实现指标阈值配置 | `LogService.java`（getMetricsConfig/updateMetricsConfig） |
| 🟢 P2 | log | 搭建 Vector | 采集各服务日志到 ClickHouse |
| 🟢 P2 | log | 接入 WebMvcConfig | 注册 TraceIdFilter + AccessLogInterceptor |

**文件清单**：

```
log-service/
├── controller/
│   └── LogController.java           ← 联调
├── service/
│   └── LogService.java              ← 实现所有 TODO
└── config/
    └── WebMvcConfig.java            ← 新增拦截器配置
```

---

## 三、协作流程

### 3.1 开发阶段（第1-2周）

```
第1周：
  ├── 所有人：确认 common 依赖可用，项目能编译通过
  ├── A：搭建 Redis，验证 topbiz 完整链路
  ├── B：创建 user、user_auth 表，实现注销/权限查询
  ├── C：创建 audit_log 表，实现审计日志持久化
  └── A+B+C：各自服务接入 WebMvcConfig（TraceIdFilter + AccessLogInterceptor）

第2周：
  ├── A：实现验证码逻辑（topbiz + message-service）
  ├── B：实现管理员 CRUD（用户/分组/权限）
  ├── C：搭建 ClickHouse + Vector，实现运维日志查询
  └── 三人联调：注册→登录→欢迎信→审计日志 全链路
```

### 3.2 联调流程

```
1. B 先完成 user-service 的接口 → A 用 topbiz 调用验证
2. C 先完成 log-service 的接口 → A 用 topbiz 调用验证
3. A 先完成 message-service 的接口 → A 自己用 topbiz 验证
4. 全部完成后，三人一起端到端测试
```

### 3.3 Git 工作流

```
main 分支：保持稳定，只合并已验证的功能

开发分支：
  feature/topbiz-message    ← A
  feature/user-service      ← B
  feature/log-service       ← C

提交规范：
  feat: 注册接口支持验证码
  fix: 修复登录 BCrypt 校验失败
  docs: 更新 API 文档
  refactor: 重构 UserService
```

---

## 四、接口联调顺序

```
阶段1：核心链路（三人一起）
  POST /api/v1/register → user-service → message-service → log-service
  POST /api/v1/login    → user-service → log-service

阶段2：验证码链路（A 主导）
  POST /api/v1/register/email_code → message-service（Redis）
  POST /api/v1/register/phone_code → message-service（Redis）

阶段3：管理功能（B 主导）
  POST /api/v1/admin/users    → user-service
  POST /api/v1/admin/groups   → user-service

阶段4：消息功能（A 主导）
  POST /api/v1/msg/carriers   → message-service
  POST /api/v1/send/instant   → message-service

阶段5：日志功能（C 主导）
  GET /api/v1/log             → log-service（MySQL）
  GET /api/v1/log/ops/query   → log-service（ClickHouse）
```

---

## 五、环境依赖

| 组件 | 版本 | 负责搭建 |
|------|------|----------|
| MySQL | 8.0+ | B（user 表）、A（message 表）、C（log 表） |
| Redis | 7.0+ | A |
| ClickHouse | 23.x+ | C |
| Vector | 0.35+ | C |

---

## 六、参考资料

| 文档 | 路径 | 用途 |
|------|------|------|
| API 接口定义 | `API.md` | 接口路径、参数、响应 |
| 数据模型设计 | `DATA_MODEL.md` | 表结构、字段说明 |
| 技术决策记录 | `ADR.md` | BCrypt/Shiro/HTTP Interface/日志 决策 |
| 日志服务设计 | 飞书 `logservice.md` | ClickHouse 表、指标定义 |
| 消息服务设计 | 飞书 `msgservice.md` | 模板、载体设计 |
| 用户服务设计 | 飞书 `userservice.md` | 用例、泳道图 |

---
