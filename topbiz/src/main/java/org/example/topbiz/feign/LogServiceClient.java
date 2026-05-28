package org.example.topbiz.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

@FeignClient(name = "log-service", url = "http://localhost:8083")
public interface LogServiceClient {

    @PostMapping("/internal/log/record")
    Map<String, Object> recordAudit(@RequestBody Map<String, Object> request);
}