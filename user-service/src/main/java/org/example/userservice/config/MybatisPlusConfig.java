package org.example.userservice.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    /**
     * 替代 MyBatis Plus 默认的 DDL runner（因数据库不可用导致 NullBean）。
     * 数据库表由 README.md 中的 SQL 手动管理，无需自动 DDL。
     */
    @Bean
    public ApplicationRunner ddlApplicationRunner() {
        return (ApplicationArguments args) -> {
            // no-op: DDL is managed manually via README.sql
        };
    }
}
