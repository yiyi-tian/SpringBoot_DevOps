package org.example.topbiz.controller;

import org.example.common.Result;
import org.example.topbiz.service.LogService;
import org.example.topbiz.support.SecurityUtilsHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class LogController {

    @Autowired
    private LogService logService;

    @GetMapping("/log")
    public Result<Map<String, Object>> queryUserAudit(@RequestParam Map<String, Object> params,
                                                       @RequestParam(required = false) Long userId) {
        Long operatorId = SecurityUtilsHelper.getCurrentUserId();
        if (operatorId == null) {
            return Result.error(401, "未登录");
        }
        Long targetUserId = userId != null ? userId : operatorId;
        return Result.ok(logService.queryUserAudit(targetUserId, params));
    }

    @GetMapping("/log/ops/query")
    public Result<Map<String, Object>> queryOpsLogs(@RequestParam Map<String, Object> params) {
        return Result.ok(logService.queryOpsLogs(params));
    }

    @PostMapping("/log/ops/query")
    public Result<Map<String, Object>> queryOpsLogsPost(@RequestBody Map<String, Object> request) {
        return Result.ok(logService.queryOpsLogsPost(request));
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
