package org.example.topbiz.config;

import org.example.topbiz.feign.LogServiceClient;
import org.example.topbiz.feign.MessageServiceClient;
import org.example.topbiz.feign.UserServiceClient;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

@Configuration
public class HttpInterfaceConfig {

    @Bean
    public UserServiceClient userServiceClient() {
        return createClient(UserServiceClient.class, "http://localhost:8081");
    }

    @Bean
    public MessageServiceClient messageServiceClient() {
        return createClient(MessageServiceClient.class, "http://localhost:8082");
    }

    @Bean
    public LogServiceClient logServiceClient() {
        return createClient(LogServiceClient.class, "http://localhost:8083");
    }

    private <T> T createClient(Class<T> clazz, String baseUrl) {
        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .filter((request, next) -> {
                    // 透传 X-Trace-Id
                    String traceId = MDC.get("traceId");
                    if (traceId != null) {
                        request.headers().set("X-Trace-Id", traceId);
                    }
                    request.headers().set("Content-Type", "application/json");
                    return next.exchange(request);
                })
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builder(WebClientAdapter.forClient(webClient))
                .blockTimeout(Duration.ofSeconds(10))
                .build();

        return factory.createClient(clazz);
    }
}