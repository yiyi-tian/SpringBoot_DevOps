package org.example.logservice.config;

import com.clickhouse.jdbc.ClickHouseDataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties(ClickHouseProperties.class)
public class ClickHouseConfig {

    @Bean("clickHouseDataSource")
    DataSource clickHouseDataSource(ClickHouseProperties properties) throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", properties.getUsername());
        props.setProperty("password", properties.getPassword());
        return new ClickHouseDataSource(properties.getUrl(), props);
    }
}
