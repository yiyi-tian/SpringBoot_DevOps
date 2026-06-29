package org.example.common;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(InternalAuthProperties.class)
public class InternalAuthConfiguration {

    @Bean
    FilterRegistrationBean<InternalAuthFilter> internalAuthFilter(InternalAuthProperties properties,
                                                                    Environment environment) {
        FilterRegistrationBean<InternalAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new InternalAuthFilter(properties, environment));
        registration.addUrlPatterns("/internal/*");
        registration.setOrder(2);
        return registration;
    }
}
