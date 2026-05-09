# SpringBoot_DevOps
2026春-SpringBoot微服务设计与DevOps实践

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
curl -X POST "http://localhost:8080/api/v1/register" -d "phone=13800000000&password=123456"  
