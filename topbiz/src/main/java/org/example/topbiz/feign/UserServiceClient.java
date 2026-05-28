package org.example.topbiz.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

@FeignClient(name = "user-service", url = "http://localhost:8081")
public interface UserServiceClient {

    @PostMapping("/internal/user/register")
    Map<String, Object> register(@RequestBody Map<String, Object> request);

    @PostMapping("/internal/user/login")
    Map<String, Object> login(@RequestBody Map<String, Object> request);
}