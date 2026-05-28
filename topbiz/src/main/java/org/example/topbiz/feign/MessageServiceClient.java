package org.example.topbiz.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

@FeignClient(name = "message-service", url = "http://localhost:8082")
public interface MessageServiceClient {

    @PostMapping("/internal/messages/instant")
    Map<String, Object> sendInstant(@RequestBody Map<String, Object> request);
}