package org.example.common;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(AccessLogProperties.class)
@ConditionalOnProperty(prefix = "devops.access-log", name = "service-name")
public class AccessLogConfiguration {

    @Bean
    AccessLogFileWriter accessLogFileWriter(AccessLogProperties properties) {
        return new AccessLogFileWriter(properties);
    }

    @Bean
    AccessLogInterceptor accessLogInterceptor(AccessLogProperties properties, AccessLogFileWriter fileWriter) {
        return new AccessLogInterceptor(properties.getServiceName(), properties, fileWriter);
    }

    @Bean
    AccessLogRetentionCleaner accessLogRetentionCleaner(AccessLogProperties properties) {
        return new AccessLogRetentionCleaner(properties);
    }

    @Bean
    FilterRegistrationBean<AccessLogBodyCaptureFilter> accessLogBodyCaptureFilter() {
        FilterRegistrationBean<AccessLogBodyCaptureFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AccessLogBodyCaptureFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
