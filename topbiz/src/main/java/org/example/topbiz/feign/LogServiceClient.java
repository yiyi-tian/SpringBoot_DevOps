package org.example.topbiz.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@FeignClient(name = "log-service", url = "http://localhost:8083")
public interface LogServiceClient {

    @PostMapping("/internal/log/record")
    Map<String, Object> recordAudit(@RequestBody Map<String, Object> request);

    @GetMapping("/internal/log/{userId}/query")
    Map<String, Object> queryUserAudit(@PathVariable Long userId, @RequestParam Map<String, Object> params);

    @GetMapping("/internal/log/ops/query")
    Map<String, Object> queryOpsLogs(@RequestParam Map<String, Object> params);

    @GetMapping("/internal/log/metrics")
    Map<String, Object> queryMetrics(@RequestParam Map<String, Object> params);

    @PostMapping("/internal/log/ops/export")
    Map<String, Object> exportLogs(@RequestBody Map<String, Object> request);

    @GetMapping("/internal/log/metrics/config")
    Map<String, Object> getMetricsConfig();

    @PutMapping("/internal/log/metrics/config")
    Map<String, Object> updateMetricsConfig(@RequestBody Map<String, Object> request);
}