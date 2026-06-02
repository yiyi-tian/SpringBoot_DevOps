package org.example.topbiz.service;

import org.example.topbiz.feign.LogServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 日志服务：审计查询、运维日志、指标的编排
 */
@Service
public class LogService {

    @Autowired
    private LogServiceClient logServiceClient;

    /**
     * 允许的导出格式
     */
    private static final Set<String> ALLOWED_EXPORT_FORMATS = new HashSet<>(Arrays.asList("csv", "json", "txt"));

    // ==================== 审计日志 ====================

    /**
     * 查询用户审计日志
     */
    public Map<String, Object> queryUserAudit(Long userId, Map<String, Object> params) {
        // 默认分页参数
        if (!params.containsKey("page")) {
            params.put("page", 1);
        }
        if (!params.containsKey("size")) {
            params.put("size", 20);
        }
        return logServiceClient.queryUserAudit(userId, params);
    }

    // ==================== 运维日志 ====================

    /**
     * 查询运维访问日志（ClickHouse）
     */
    public Map<String, Object> queryOpsLogs(Map<String, Object> params) {
        // 校验时间范围
        if (!params.containsKey("start_time")) {
            // 默认最近1小时
            params.put("start_time", System.currentTimeMillis() - 3600000);
        }
        if (!params.containsKey("end_time")) {
            params.put("end_time", System.currentTimeMillis());
        }

        // 默认分页
        if (!params.containsKey("page")) {
            params.put("page", 1);
        }
        if (!params.containsKey("size")) {
            params.put("size", 20);
        }

        // 校验 service_name（如果传入）
        if (params.containsKey("service_name")) {
            String serviceName = String.valueOf(params.get("service_name"));
            Set<String> validServices = new HashSet<>(Arrays.asList("topbiz", "user", "message", "log"));
            if (!validServices.contains(serviceName)) {
                params.put("service_name", null); // 无效则忽略
            }
        }

        return logServiceClient.queryOpsLogs(params);
    }

    // ==================== 指标查询 ====================

    /**
     * 查询日志指标（ClickHouse 实时聚合）
     */
    public Map<String, Object> queryMetrics(Map<String, Object> params) {
        // 校验必填参数 metric
        if (!params.containsKey("metric") || String.valueOf(params.get("metric")).isEmpty()) {
            throw new IllegalArgumentException("metric 参数不能为空");
        }

        // 校验 metric 值是否合法
        String metric = String.valueOf(params.get("metric"));
        Set<String> validMetrics = new HashSet<>(Arrays.asList(
                "qps", "pv", "api_calls", "slow_count",
                "error_rate", "error_count", "http_5xx", "http_4xx",
                "avg", "p95", "p99", "max", "slowest_api",
                "success_rate",
                "ip_request_topn", "ip_error_topn"
        ));
        if (!validMetrics.contains(metric)) {
            throw new IllegalArgumentException("无效的 metric: " + metric);
        }

        // 默认时间范围
        if (!params.containsKey("start_time")) {
            params.put("start_time", System.currentTimeMillis() - 3600000);
        }
        if (!params.containsKey("end_time")) {
            params.put("end_time", System.currentTimeMillis());
        }

        // 默认 top_n
        if (!params.containsKey("top_n")) {
            params.put("top_n", 10);
        }

        return logServiceClient.queryMetrics(params);
    }

    // ==================== 日志导出 ====================

    /**
     * 导出日志
     */
    public Map<String, Object> exportLogs(Map<String, Object> request) {
        // 校验导出格式
        if (!request.containsKey("format")) {
            request.put("format", "csv"); // 默认 CSV
        }
        String format = String.valueOf(request.get("format")).toLowerCase();
        if (!ALLOWED_EXPORT_FORMATS.contains(format)) {
            throw new IllegalArgumentException("不支持的导出格式: " + format + "，支持: csv, json, txt");
        }

        // 默认时间范围（导出最近24小时）
        if (!request.containsKey("start_time")) {
            request.put("start_time", System.currentTimeMillis() - 86400000);
        }
        if (!request.containsKey("end_time")) {
            request.put("end_time", System.currentTimeMillis());
        }

        return logServiceClient.exportLogs(request);
    }

    // ==================== 指标配置 ====================

    public Map<String, Object> getMetricsConfig() {
        return logServiceClient.getMetricsConfig();
    }

    public Map<String, Object> updateMetricsConfig(Map<String, Object> request) {
        // 校验阈值类型
        if (request.containsKey("config_key")) {
            String configKey = String.valueOf(request.get("config_key"));
            Set<String> validKeys = new HashSet<>(Arrays.asList(
                    "error_rate_max", "p99_max", "success_rate_min", "slow_count_max"
            ));
            if (!validKeys.contains(configKey)) {
                throw new IllegalArgumentException("无效的配置项: " + configKey);
            }
        }
        return logServiceClient.updateMetricsConfig(request);
    }
}