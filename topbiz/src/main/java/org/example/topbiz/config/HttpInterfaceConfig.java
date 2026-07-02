package org.example.topbiz.config;

import org.example.common.InternalAuthFilter;
import org.example.common.InternalAuthProperties;
import org.example.topbiz.feign.LogServiceClient;
import org.example.topbiz.feign.MessageServiceClient;
import org.example.topbiz.feign.UserServiceClient;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties({DevopsServiceProperties.class, DevopsMessagingProperties.class, InternalAuthProperties.class})
public class HttpInterfaceConfig {

    private final DevopsServiceProperties serviceProperties;
    private final InternalAuthProperties internalAuthProperties;

    public HttpInterfaceConfig(DevopsServiceProperties serviceProperties,
                               InternalAuthProperties internalAuthProperties) {
        this.serviceProperties = serviceProperties;
        this.internalAuthProperties = internalAuthProperties;
    }

    @Bean
    public UserServiceClient userServiceClient() {
        return createClient(UserServiceClient.class, serviceProperties.getUserUrl());
    }

    @Bean
    public MessageServiceClient messageServiceClient() {
        return createClient(MessageServiceClient.class, serviceProperties.getMessageUrl());
    }

    @Bean
    public LogServiceClient logServiceClient() {
        return createClient(LogServiceClient.class, serviceProperties.getLogUrl());
    }

    private <T> T createClient(Class<T> clazz, String baseUrl) {
        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .filter((request, next) -> {
                    ClientRequest.Builder builder = ClientRequest.from(request);
                    String traceId = MDC.get("traceId");
                    if (traceId != null) {
                        builder.header("X-Trace-Id", traceId);
                    }
                    if (!request.headers().containsKey("Content-Type")) {
                        builder.header("Content-Type", MediaType.APPLICATION_JSON_VALUE);
                    }
                    if (internalAuthProperties.isEnforcementEnabled()) {
                        builder.header(InternalAuthFilter.HEADER_NAME, internalAuthProperties.getToken());
                    }
                    return next.exchange(builder.build());
                })
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builder(WebClientAdapter.forClient(webClient))
                .blockTimeout(Duration.ofSeconds(20))
                .build();

        return factory.createClient(clazz);
    }
}
