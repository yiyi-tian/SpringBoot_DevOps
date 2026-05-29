# SpringBoot_DevOps
2026春-SpringBoot微服务设计与DevOps实践

## 文档说明
* 统一接口文档：[API.md](./docs/API.md)
* 技术决策记录：[ADR.md](./docs/ADR.md)
* 数据模型设计：[DATA_MODEL.md](./docs/DATA_MODEL.md)
* 分工：[COLLABORATION.md](./docs/COLLABORATION.md)

## 配置文件说明 
* 路径：/SpringBoot_DevOps/user-service/src/main/resources/
* 作用：端口和数据库配置
### application.yml
公共配置文件，用于指定当前激活环境。

### application-dev.yml
开发环境本地配置文件。

### application-dev-example.yml
开发环境配置模板文件，由于每个人开发环境不同，请参照此模板对application-dev.yml进行配置。

## 项目启动流程
### Linux
目前开发阶段未打jar包，按以下顺序启动：
1. 编译整个项目：  
cd ~/SpringBoot_DevOps  
mvn clean install

2. 确保MySQL启动，application-dev.yml对数据库的配置需与系统一致，创建数据库表

3. user-service ：终端1  
cd ~/SpringBoot_DevOps/user-service  
mvn spring-boot:run  

4. message-service：终端2  
cd ~/SpringBoot_DevOps/message-service  
mvn spring-boot:run  

5. log-service：终端3  
cd ~/SpringBoot_DevOps/log-service  
mvn spring-boot:run  

6. topbiz：终端4  
cd ~/SpringBoot_DevOps/topbiz  
mvn spring-boot:run  

### 测试示例
#### 注册
curl -X POST "http://localhost:8080/api/v1/register" -H "Content-Type: application/json" -d "{\"phone\":\"13800000001\", \"password\":\"123456\"}"

#### 登录
curl -X POST "http://localhost:8080/api/v1/login" -H "Content-Type: application/json" -d "{\"phone\":\"13800000001\", \"password\":\"123456\"}"

## 数据库存储
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

```sql

```

### devops_log

```sql

```