# 变更汇总

> 本阶段实现与修复记录（Log 域、Session、本地开发环境对齐）。  
> 接口规格见 [API.md](./API.md)，技术决策见 [ADR.md](./ADR.md)，缺口见 [GAPS.md](./GAPS.md)。  
> 新同学上手见 [DEVELOPMENT.md](./DEVELOPMENT.md)。

## 1. 背景与架构结论

- **对外唯一入口**：TopBiz（8080），客户端只访问 `/api/v1/*`。
- **会话**：TopBiz + Apache Shiro 2（Jakarta）；Session 持久化在 **Spring Session Data Redis**（namespace `shiro:session`，Cookie `JSESSIONID`）。
- **本地 IDE 开发环境**：
  - **MySQL / Redis**：云端服务器（地址与密码**向组长索取**，写入本地 `application-dev.yml`，勿提交 Git）
  - **ClickHouse + Vector**：本机 Docker（`[infra/docker-compose.local.yml](../infra/docker-compose.local.yml)`）

## 2. 代码与配置变更


| 模块               | 主要改动                                                                                                                                         | 关键文件                                                                                                                                                                               |
| ---------------- | -------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **topbiz**       | Shiro 2 Jakarta；显式 `Authorizer`/`Authenticator`；Spring Session Redis；Log API 代理；`/api/v1/log/`** 需 `roles[admin]`；WebClient 请求头修复；注册/登录错误码映射 | `ShiroConfig`, `RedisSessionConfig`, `ShiroRealm`, `LogController`, `LogService`, `HttpInterfaceConfig`, `AuthController`, `GlobalExceptionHandler`, `pom.xml`, `application*.yml` |
| **user-service** | MyBatis-Plus 改为 `mybatis-plus-spring-boot3-starter` 3.5.7；`getPermissions` 最小 admin 判定                                                       | `pom.xml`, `UserService.java`                                                                                                                                                      |
| **log-service**  | MyBatis-Plus Boot3；`application-dev-example.yml`（云 MySQL + 本地 ClickHouse）                                                                    | `pom.xml`, `application-dev-example.yml`                                                                                                                                           |
| **common**       | 访问日志 JSONL 写盘；query/body 脱敏；jsonl 保留清理                                                                                                       | `AccessLogMasker`, `AccessLogRetentionCleaner`, `AccessLogConfiguration`, `AccessLogFileWriter`                                                                                    |
| **infra**        | TopBiz Docker profile；本地仅 CH+Vector 的 compose                                                                                                | `docker-compose.yml`, `docker-compose.local.yml`, `vector/vector.toml`                                                                                                             |


## 3. 已修复问题


| 现象                                                     | 原因                                          | 处理                                                 |
| ------------------------------------------------------ | ------------------------------------------- | -------------------------------------------------- |
| TopBiz 启动 `NoClassDefFoundError: javax/servlet/Filter` | Shiro 1.x / shiro-redis 与 Spring Boot 3 不兼容 | 升级 Shiro 2 Jakarta；Session 改 Spring Session Redis  |
| TopBiz 启动 `No bean named 'authorizer'`                 | Shiro 2 需显式注册 Authorizer                    | `ShiroConfig` 增加 `Authorizer`/`Authenticator` Bean |
| 注册返回 500                                               | TopBiz Redis 指向 localhost，本地未起 Redis        | `application-dev.yml` 配置云端 Redis                   |
| 注册仍 500（Redis 已通）                                      | `HttpInterfaceConfig` 对只读 headers 调用 `set`  | 改用 `ClientRequest.from(...).header(...).build()`   |
| 409 已注册却显示 `code:0`                                    | `AuthController` 一律 `Result.ok(result)`     | 增加 `fromServiceResult` 透传内部 `code`                 |
| user-service 启动 `ddlApplicationRunner`                 | MyBatis-Plus Boot2 starter 与 SB3 不兼容        | 改为 `mybatis-plus-spring-boot3-starter` 3.5.7       |


## 4. 目录说明：infra / shared-logs / logs


| 目录                                | 作用                                          | 何时使用                                                                                                                 |
| --------------------------------- | ------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| `[infra/](../infra/)`             | Docker Compose、Vector 配置、初始化/测试脚本           | 本地起 ClickHouse/Vector；或全栈 Docker 部署                                                                                  |
| `[shared-logs/](../shared-logs/)` | IDE `mvn spring-boot:run` 时访问日志 JSONL 输出根目录 | 配置项 `devops.access-log.output-dir`（相对路径 `shared-logs/access`），实际文件在 `{output-dir}/{serviceName}/access-{date}.jsonl` |
| `[logs/](../logs/)`               | `.gitignore` 忽略；Docker 容器内相对路径              | 全服务跑在 Docker 时使用 compose 卷 `access_logs`                                                                             |


**数据流（本地 IDE 开发）**：

```
Java 服务 → shared-logs/access/{service}/access-*.jsonl
         → Vector（docker-compose.local 挂载 shared-logs/access）
         → ClickHouse devops.access_log
```

**注意**：API 文档中「Docker 全栈」场景写 `logs/access/`；「IDE 本地」场景写 `shared-logs/access/`，二者结构均为 `{serviceName}/access-{date}.jsonl`。

## 5. 配置模板

各服务从 `application-dev-example.yml` 复制为 `application-dev.yml`（已在 `.gitignore`，勿提交密码）：


| 服务           | 模板                                             | 要点                                                    |
| ------------ | ---------------------------------------------- | ----------------------------------------------------- |
| user-service | `user-service/.../application-dev-example.yml` | 占位符 `your_mysql_host` → 库 `devops_user`               |
| topbiz       | `topbiz/.../application-dev-example.yml`       | 占位符 `your_redis_host`（Session）                        |
| log-service  | `log-service/.../application-dev-example.yml`  | 云 MySQL `devops_log` + 本地 ClickHouse `localhost:8123` |


## 6. 协作者须知


| 类型            | 路径                                                  | 说明                       |
| ------------- | --------------------------------------------------- | ------------------------ |
| **每人本地维护**    | `**/application-dev.yml`                            | 含真实密码与主机，已在 `.gitignore` |
| **仓库模板（可提交）** | `**/application-dev-example.yml`                    | 仅占位符，clone 后复制改名         |
| **勿提交**       | `target/`、`.idea/`、`shared-logs/**/*.jsonl`、`logs/` | 编译产物、IDE 配置、运行时日志        |
| **可提交占位**     | `shared-logs/access/.gitkeep`                       | 保留目录结构                   |


访问日志 `output-dir` 使用相对路径 `shared-logs/access`；**必须在各服务模块目录下**执行 `mvn spring-boot:run`（工作目录为模块根）。

## 7. 管理员与测试

**admin 判定**（dev 最小实现）：

- `就是userId == 1也是：`
  ```postman_json
  {
      "phone":"13800000001", "password":"123456"
  }
  ```
  ，或
- `user_auth.identifier == "admin"`

**最小联调顺序**：

1. `docker compose -f infra/docker-compose.local.yml up -d`（ClickHouse + Vector）
2. 启动 user-service（8081）、topbiz（8080）
3. `POST /api/v1/register` → `POST /api/v1/login`
4. （可选）云端 Redis：`KEYS shiro:session:`*
5. admin 登录后：`POST /api/v1/log/ops/query`（需 log-service 8083）

## 8. 已知限制与后续

- message-service 尚无 datasource；验证码、载体等待实现。
- 完整 RBAC 权限表、管理员 CRUD 待完善。
- WebSocket 监控、Flyway 迁移等见 [GAPS.md](./GAPS.md)。

