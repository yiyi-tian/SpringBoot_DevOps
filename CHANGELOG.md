# Changelog

本文件记录项目里程碑变更。格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。

## [0.2.0] - 2026-06-02

### Added

- **Log 域**：log-service ClickHouse 查询/指标/导出、审计 MySQL 持久化、TopBiz Log API 代理（admin 鉴权）
- **Session**：TopBiz Shiro 2（Jakarta）+ Spring Session Data Redis（namespace `shiro:session`）
- **访问日志管道**：common 模块 JSONL 写盘、脱敏、保留清理；Vector → ClickHouse 本地 compose（`infra/docker-compose.local.yml`）
- **文档**：`docs/DEVELOPMENT.md` 协作者上手指南、`docs/变更汇总.md`、README 目录与测试说明
- **配置模板**：topbiz / log-service `application-dev-example.yml`（占位符，不含真实密码）

### Fixed

- TopBiz 注册 500：云端 Redis 配置、WebClient 只读 Header 修复
- AuthController 409/400 错误码透传
- user-service MyBatis-Plus Boot3 兼容（`mybatis-plus-spring-boot3-starter` 3.5.7）

### Changed

- Maven 版本升至 `0.2.0-SNAPSHOT`
- Access log path fix: IDE `../shared-logs/access`, Docker `logs/access` + shared volume

## [0.0.1] - 更早

- 项目骨架：TopBiz BFF、user/message/log 微服务、common 模块、HTTP Interface 替代 OpenFeign

[0.2.0]: https://github.com/your-org/SpringBoot_DevOps/compare/v0.0.1...v0.2.0
