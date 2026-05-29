package org.example.topbiz.controller;

import org.example.common.Result;
import org.example.topbiz.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class LogController {

    @Autowired
    private LogService logService;

    @GetMapping("/log")
    public Result<Map<String, Object>> queryUserAudit(@RequestParam Map<String, Object> params) {
        // TODO: 从 Shiro 会话获取当前 userId
        Long userId = 1L;
        return Result.ok(logService.queryUserAudit(userId, params));
    }

    @GetMapping("/log/ops/query")
    public Result<Map<String, Object>> queryOpsLogs(@RequestParam Map<String, Object> params) {
        return Result.ok(logService.queryOpsLogs(params));
    }

    @GetMapping("/log/metrics")
    public Result<Map<String, Object>> queryMetrics(@RequestParam Map<String, Object> params) {
        return Result.ok(logService.queryMetrics(params));
    }

    @PostMapping("/log/ops/export")
    public Result<Map<String, Object>> exportLogs(@RequestBody Map<String, Object> request) {
        return Result.ok(logService.exportLogs(request));
    }

    @GetMapping("/log/metrics/config")
    public Result<Map<String, Object>> getMetricsConfig() {
        return Result.ok(logService.getMetricsConfig());
    }

    @PutMapping("/log/metrics/config")
    public Result<Map<String, Object>> updateMetricsConfig(@RequestBody Map<String, Object> request) {
        return Result.ok(logService.updateMetricsConfig(request));
    }
}