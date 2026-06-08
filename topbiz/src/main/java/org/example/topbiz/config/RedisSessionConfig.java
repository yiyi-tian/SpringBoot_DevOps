package org.example.topbiz.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@EnableRedisHttpSession(
        redisNamespace = "shiro:session",
        maxInactiveIntervalInSeconds = 1800
)
public class RedisSessionConfig {
}
