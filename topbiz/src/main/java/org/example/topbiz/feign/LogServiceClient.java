package org.example.topbiz.feign;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.*;

import java.util.Map;

@HttpExchange("/internal")
public interface LogServiceClient {

    @PostExchange("/log/record")
    Map<String, Object> recordAudit(@RequestBody Map<String, Object> request);

    @GetExchange("/log/{userId}/query")
    Map<String, Object> queryUserAudit(@PathVariable Long userId, @RequestParam Map<String, Object> params);

    @GetExchange("/log/ops/query")
    Map<String, Object> queryOpsLogs(@RequestParam Map<String, Object> params);

    @PostExchange("/log/ops/query")
    Map<String, Object> queryOpsLogsPost(@RequestBody Map<String, Object> request);

    @GetExchange("/log/metrics")
    Map<String, Object> queryMetrics(@RequestParam Map<String, Object> params);

    @PostExchange("/log/ops/export")
    Map<String, Object> exportLogs(@RequestBody Map<String, Object> request);

    @GetExchange("/log/metrics/config")
    Map<String, Object> getMetricsConfig();

    @PutExchange("/log/metrics/config")
    Map<String, Object> updateMetricsConfig(@RequestBody Map<String, Object> request);
}