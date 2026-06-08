package org.example.logservice.controller;

import org.example.logservice.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class LogController {

    @Autowired
    private LogService logService;

    // ==================== 审计日志 ====================
    @PostMapping("/internal/log/record")
    public Map<String, Object> recordAudit(@RequestBody Map<String, Object> request) {
        return logService.recordAudit(request);
    }

    @GetMapping("/internal/log/{userId}/query")
    public Map<String, Object> queryUserAudit(@PathVariable Long userId, @RequestParam Map<String, Object> params) {
        return logService.queryUserAudit(userId, params);
    }

    // ==================== 运维日志 ====================
    @GetMapping("/internal/log/ops/query")
    public Map<String, Object> queryOpsLogs(@RequestParam Map<String, Object> params) {
        return logService.queryOpsLogs(params);
    }

    @PostMapping("/internal/log/ops/query")
    public Map<String, Object> queryOpsLogsPost(@RequestBody Map<String, Object> request) {
        return logService.queryOpsLogs(request);
    }

    // ==================== 指标查询 ====================
    @GetMapping("/internal/log/metrics")
    public Map<String, Object> queryMetrics(@RequestParam Map<String, Object> params) {
        return logService.queryMetrics(params);
    }

    // ==================== 日志导出 ====================
    @PostMapping("/internal/log/ops/export")
    public Map<String, Object> exportLogs(@RequestBody Map<String, Object> request) {
        return logService.exportLogs(request);
    }

    // ==================== 指标配置 ====================
    @GetMapping("/internal/log/metrics/config")
    public Map<String, Object> getMetricsConfig() {
        return logService.getMetricsConfig();
    }

    @PutMapping("/internal/log/metrics/config")
    public Map<String, Object> updateMetricsConfig(@RequestBody Map<String, Object> request) {
        return logService.updateMetricsConfig(request);
    }
}