# SpringBoot_DevOps
2026春-SpringBoot微服务设计与DevOps实践

**当前里程碑：[v0.2.0](CHANGELOG.md)** — Log 域 + Spring Session Redis + 本地 CH/Vector 开发链路。变更见 [CHANGELOG.md](./CHANGELOG.md) 与 [变更汇总.md](./docs/变更汇总.md)。

## 5 分钟上手

1. **Clone 并编译**：`mvn clean install`
2. **配置**：复制 `application-dev-example.yml` → `application-dev.yml`（user-service、topbiz、log-service），MySQL/Redis 地址**向组长索取**后填入
3. **启动**：`docker compose -f infra/docker-compose.local.yml up -d`，再在各服务目录 `mvn spring-boot:run`（8081→8082→8083→8080）
4. **（可选）演示前端**：`cd frontend && npm install && npm run dev`，打开 http://localhost:5173

详细步骤、FAQ、Git 提交说明见 **[开发指南 DEVELOPMENT.md](./docs/DEVELOPMENT.md)**。

## 文档说明

| 文档 | 说明 |
|------|------|
| [DEVELOPMENT.md](./docs/DEVELOPMENT.md) | **协作者上手（推荐先读）** |
| [API.md](./docs/API.md) | 统一接口规格 |
| [ADR.md](./docs/ADR.md) | 技术决策记录 |
| [DATA_MODEL.md](./docs/DATA_MODEL.md) | 数据模型设计 |
| [GAPS.md](./docs/GAPS.md) | 文档与实现缺口 |
| [COLLABORATION.md](./docs/COLLABORATION.md) | 分工与协作 |
| [变更汇总.md](./docs/变更汇总.md) | 本阶段代码/配置变更与测试要点 |
| [CHANGELOG.md](./CHANGELOG.md) | 版本发布记录 |
## 目录说明：infra / shared-logs / logs

| 目录 | 作用 | 何时使用 |
|------|------|----------|
| [infra/](./infra/) | DevOps 基础设施：`docker-compose.yml`（全栈）、**`docker-compose.local.yml`（推荐本地：仅 CH+Vector）**、[vector/vector.toml](./infra/vector/vector.toml)、初始化/测试脚本 | 本地起 ClickHouse/Vector；或一键 Docker 部署全服务 |
| [shared-logs/](./shared-logs/) | **IDE 开发**：四服务统一写入仓库根 `shared-logs/access/{serviceName}/`（配置 `output-dir: ../shared-logs/access`，须在**各模块目录**下启动） | local compose 挂载此目录给 Vector |
| [logs/](./logs/) | **Docker 全栈**：容器内 `logs/access/{serviceName}/`（`application-docker.yml`）；compose 卷 `access_logs` 四服务与 Vector **共享** | 全服务跑在 Docker 时使用 |

**Vector 说明**：Vector 是 **infra 独立容器**，不归属 log-service。log-service 只**查询** ClickHouse；采集靠 **共享目录/卷**（IDE 绑 `shared-logs/access`，Docker 用 `access_logs` 卷）。

**数据流（IDE + local compose）**：

```
各模块 mvn spring-boot:run → 仓库根 shared-logs/access/{service}/access-*.jsonl
                            → Vector（挂载 shared-logs/access → /var/log/access）
                            → ClickHouse
```

**数据流（全栈 Docker）**：

```
topbiz/user/message/log 容器 → 写入 access_logs 卷 /app/logs/access/{service}/
Vector 容器                  → 只读同一卷 /var/log/access/
                            → ClickHouse
```

## 配置文件说明

各服务 `src/main/resources/` 下：

| 文件 | 说明 |
|------|------|
| `application.yml` | 公共配置，指定激活 profile |
| `application-dev.yml` | 开发环境本地配置（**已在 .gitignore，勿提交密码**） |
| `application-dev-example.yml` | 配置模板，复制后改名为 `application-dev.yml` 并填写 |
| `application-docker.yml` | Docker Compose / K8s 容器内配置（服务间 DNS、内部 token） |

**配置模板清单**：

| 服务 | 模板路径 | 要点 |
|------|----------|------|
| user-service | `user-service/src/main/resources/application-dev-example.yml` | 占位符 `your_mysql_host` → 库 `devops_user` |
| topbiz | `topbiz/src/main/resources/application-dev-example.yml` | Redis + `devops.services.*` 默认 localhost |
| log-service | `log-service/src/main/resources/application-dev-example.yml` | 云 MySQL `devops_log` + 本地 ClickHouse `localhost:8123` |
| message-service | `message-service/src/main/resources/application-dev-example.yml` | 云 MySQL `devops_message` |

各服务 IDE 配置项 `devops.access-log.output-dir` 为 **`../shared-logs/access`**（相对各模块目录，汇聚到仓库根）。Docker profile 使用 **`logs/access`**（写入 `access_logs` 共享卷）。

### 内部服务调用（dev / docker / K8s）

| 环境 | topbiz → user-service | 内部鉴权 |
|------|----------------------|----------|
| IDE 本地 | `http://localhost:8081` | 可不配置 token（dev profile 跳过） |
| Docker Compose | `http://user-service:8081` | `DEVOPS_INTERNAL_TOKEN`（compose 默认 `dev-internal-secret`） |
| Kubernetes | `http://user-service:8081` | Secret `DEVOPS_INTERNAL_TOKEN` |

对外**仅暴露 topbiz 8080**（Compose）或 Ingress（K8s）；8081–8083 不映射到宿主机。

## Docker 全栈部署（Compose）

```powershell
cd infra
# 构建镜像前先在仓库根目录: mvn clean package -DskipTests
docker compose up -d --build
```

生产建议叠加 overlay，不暴露 MySQL/Redis/ClickHouse 端口：

```powershell
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

## Kubernetes 集群部署

清单位于 [infra/k8s/](infra/k8s/README.md)。仅 topbiz 通过 Ingress 对外；user / message / log 为 ClusterIP。

```bash
kubectl apply -f infra/k8s/namespace.yaml
kubectl apply -f infra/k8s/configmap-app.yaml
cp infra/k8s/secret-app.yaml.example infra/k8s/secret-app.yaml  # 编辑后 apply
kubectl apply -R -f infra/k8s/
```

详见 [DEVELOPMENT.md §10](./docs/DEVELOPMENT.md#10-docker-与-kubernetes-部署)。

## 基础设施（本地 IDE 开发）

| 组件 | 位置 | 说明 |
|------|------|------|
| MySQL | **云端** | user / log / message 三库 |
| Redis | **云端** | TopBiz Session（Spring Session，namespace `shiro:session`） |
| ClickHouse + Vector | **本机 Docker** | 运维日志采集与查询 |

本地 Docker **只需**启动 ClickHouse 与 Vector（不要与云端重复起 mysql/redis）：

```powershell
cd infra
docker compose -f docker-compose.local.yml up -d
```

若曾启动过全栈 compose 中的本地 `devops-mysql` / `devops-redis`，可停止以免占用 3306/6379：

```powershell
docker compose stop mysql redis
```

## 项目启动流程

### Windows

1. 编译整个项目（PowerShell，项目根目录）：

```powershell
cd D:\javacode\SpringBoot_DevOps
mvn clean install
```

2. 复制各服务 `application-dev-example.yml` → `application-dev.yml`，配置云端 MySQL/Redis（见上表）。

3. 启动本机 Docker（ClickHouse + Vector）：

```powershell
cd infra
docker compose -f docker-compose.local.yml up -d
```

4. 各服务分别开终端，`mvn spring-boot:run`：

| 终端 | 目录 | 端口 |
|------|------|------|
| 1 | `user-service` | 8081 |
| 2 | `message-service` | 8082 |
| 3 | `log-service` | 8083 |
| 4 | `topbiz` | 8080 |

**端口占用排查**（Windows）：

```powershell
netstat -ano | findstr :8080
taskkill /PID <pid> /F
```

### Linux

1. 编译：`cd ~/SpringBoot_DevOps && mvn clean install`
2. 复制并配置各服务 `application-dev.yml`
3. `cd infra && docker compose -f docker-compose.local.yml up -d`
4. 按上表顺序在各服务目录执行 `mvn spring-boot:run`

## 测试示例

对外接口统一走 TopBiz（8080）。请求体推荐使用 `credential` + `password` 写法（与 [API.md](./docs/API.md) §1.5 一致）。

### 注册

**Linux / Git Bash：**

```bash
curl -X POST "http://localhost:8080/api/v1/register" \
  -H "Content-Type: application/json" \
  -d '{"credentialType":"PHONE","credential":"13800000001","password":"123456"}'
```

**Windows（PowerShell，建议 JSON 文件）：**

```powershell
@'
{"credentialType":"PHONE","credential":"13800000001","password":"123456"}
'@ | Set-Content -Encoding utf8 register.json

curl.exe -X POST "http://localhost:8080/api/v1/register" -H "Content-Type: application/json" -d "@register.json"
```

### 登录

```powershell
curl.exe -X POST "http://localhost:8080/api/v1/login" -H "Content-Type: application/json" -d "@register.json"
```

登录成功后响应头含 `Set-Cookie: JSESSIONID=...`，后续请求需携带 Cookie。

### 管理员测试（运维日志查询）

**admin 判定**（dev 最小实现）：`userId=1` 或 `user_auth.identifier=admin`。详见 [GAPS.md](./docs/GAPS.md) §2。

使用 admin 账号登录后：

```powershell
curl.exe -X POST "http://localhost:8080/api/v1/log/ops/query" `
  -H "Content-Type: application/json" `
  -H "Cookie: JSESSIONID=<从登录响应复制>" `
  -d '{"page":1,"size":10}'
```

需 log-service（8083）已启动；非 admin 返回 403。

## 提交到 GitHub

提交前请阅读 **[docs/DEVELOPMENT.md §9](./docs/DEVELOPMENT.md#9-提交到-github)**。摘要：

- **可提交**：源码、`application-dev-example.yml`（占位符）、`docs/`、`infra/`、`shared-logs/access/.gitkeep`
- **勿提交**：`**/application-dev.yml`（含密码）、`target/`、`.idea/`、`shared-logs/**/*.jsonl`

推荐 commit message：`feat: log pipeline, Spring Session Redis, and v0.2.0 dev docs`  
合并后可打 tag：`v0.2.0`

## 数据库存储
表结构详见 [DATA_MODEL.md](./docs/DATA_MODEL.md) 与 [docs/sql/](./docs/sql/)。

### devops_user

```sql
-- 创建数据库（如果还没有）
CREATE DATABASE IF NOT EXISTS devops_user DEFAULT CHARACTER SET utf8mb4;

USE devops_user;

-- 用户主体表
CREATE TABLE IF NOT EXISTS `user` (
`id` BIGINT AUTO_INCREMENT PRIMARY KEY,
`display_name` VARCHAR(128) NOT NULL COMMENT '显示名',
`status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/LOCKED/DEREGISTERED',
`is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 认证凭证表
CREATE TABLE IF NOT EXISTS `user_auth` (
`auth_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
`user_id` BIGINT NOT NULL COMMENT '关联 user.id',
`identity_type` VARCHAR(32) NOT NULL COMMENT 'PHONE/EMAIL/USERNAME',
`identifier` VARCHAR(255) NOT NULL COMMENT '手机号/邮箱/用户名',
`secret_hash` VARCHAR(255) NOT NULL COMMENT 'BCrypt 哈希',
`verified` TINYINT(1) DEFAULT 1 COMMENT '是否已验证',
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
UNIQUE KEY `uk_identity` (`identity_type`, `identifier`),
KEY `idx_user_id` (`user_id`)
);
```

### devops_message

见 [`docs/sql/03_devops_message.sql`](docs/sql/03_devops_message.sql)：

| 表 | 说明 |
|----|------|
| `msg_carrier` | 载体（IN_APP / TENCENT_SMS / EMAIL），`config_json` 存 SMTP 或 SMS 配置 |
| `msg_template` | 消息模板；种子数据 `templateId=1` 为注册欢迎站内信（IN_APP，ACTIVE） |
| `msg_message` | 发送流水（IN_APP 落库、EMAIL 发送记录） |

欢迎信 `templateId` / `channelType` 由 topbiz 配置 `devops.messaging.welcome` 指定，勿在业务代码硬编码。

### devops_log

```sql

```
/db_scripts: sql脚本程序目录
