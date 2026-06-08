package org.example.logservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MetricsAggregateProperties.class)
public class MetricsAggregateConfig {
}
