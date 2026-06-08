package org.example.topbiz.service;

import org.example.topbiz.exception.InternalServiceException;
import org.example.topbiz.feign.LogServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 日志服务：审计查询、运维日志、指标的编排
 */
@Service
public class LogService {

    private static final Set<String> ALLOWED_EXPORT_FORMATS = new HashSet<>(Arrays.asList("csv", "json", "txt"));
    private static final Set<String> VALID_SERVICES = new HashSet<>(Arrays.asList("topbiz", "user", "message", "log"));
    private static final Set<String> VALID_METRICS = new HashSet<>(Arrays.asList(
            "qps", "pv", "api_calls", "slow_count",
            "error_rate", "error_count", "http_5xx", "http_4xx",
            "avg", "p95", "p99", "max", "slowest_api",
            "success_rate",
            "ip_request_topn", "ip_error_topn"
    ));

    @Autowired
    private LogServiceClient logServiceClient;

    public Map<String, Object> queryUserAudit(Long userId, Map<String, Object> params) {
        applyPaginationDefaults(params);
        return unwrapInternalResponse(logServiceClient.queryUserAudit(userId, params));
    }

    public Map<String, Object> queryOpsLogs(Map<String, Object> params) {
        prepareOpsQueryParams(params);
        return unwrapInternalResponse(logServiceClient.queryOpsLogs(params));
    }

    public Map<String, Object> queryOpsLogsPost(Map<String, Object> request) {
        prepareOpsQueryParams(request);
        return unwrapInternalResponse(logServiceClient.queryOpsLogsPost(request));
    }

    public Map<String, Object> queryMetrics(Map<String, Object> params) {
        if (!params.containsKey("metric") || String.valueOf(params.get("metric")).isBlank()) {
            throw new IllegalArgumentException("metric 参数不能为空");
        }
        String metric = String.valueOf(params.get("metric"));
        if (!VALID_METRICS.contains(metric)) {
            throw new IllegalArgumentException("无效的 metric: " + metric);
        }
        applyTimeRangeDefaults(params);
        if (!params.containsKey("top_n")) {
            params.put("top_n", 10);
        }
        return unwrapInternalResponse(logServiceClient.queryMetrics(params));
    }

    public Map<String, Object> exportLogs(Map<String, Object> request) {
        if (!request.containsKey("format")) {
            request.put("format", "csv");
        }
        String format = String.valueOf(request.get("format")).toLowerCase();
        if (!ALLOWED_EXPORT_FORMATS.contains(format)) {
            throw new IllegalArgumentException("不支持的导出格式: " + format + "，支持: csv, json, txt");
        }
        applyTimeRangeDefaults(request);
        return unwrapInternalResponse(logServiceClient.exportLogs(request));
    }

    public Map<String, Object> getMetricsConfig() {
        return unwrapInternalResponse(logServiceClient.getMetricsConfig());
    }

    public Map<String, Object> updateMetricsConfig(Map<String, Object> request) {
        if (request.containsKey("config_key")) {
            String configKey = String.valueOf(request.get("config_key"));
            Set<String> validKeys = new HashSet<>(Arrays.asList(
                    "error_rate_max", "p99_max", "success_rate_min", "slow_count_max"
            ));
            if (!validKeys.contains(configKey)) {
                throw new IllegalArgumentException("无效的配置项: " + configKey);
            }
        }
        return unwrapInternalResponse(logServiceClient.updateMetricsConfig(request));
    }

    private void prepareOpsQueryParams(Map<String, Object> params) {
        applyTimeRangeDefaults(params);
        applyPaginationDefaults(params);
        if (params.containsKey("service_name")) {
            String serviceName = String.valueOf(params.get("service_name"));
            if (!VALID_SERVICES.contains(serviceName)) {
                params.remove("service_name");
            }
        }
    }

    private void applyTimeRangeDefaults(Map<String, Object> params) {
        if (!params.containsKey("start_time") && !params.containsKey("end_time") && !params.containsKey("time_range")) {
            params.put("time_range", "24h");
        }
    }

    private void applyPaginationDefaults(Map<String, Object> params) {
        if (!params.containsKey("page")) {
            params.put("page", 1);
        }
        if (!params.containsKey("size")) {
            params.put("size", 20);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> unwrapInternalResponse(Map<String, Object> response) {
        if (response == null) {
            throw new InternalServiceException(502, "log-service 无响应");
        }
        Object codeObj = response.get("code");
        int code = codeObj instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(codeObj));
        if (code != 0) {
            String message = String.valueOf(response.getOrDefault("message", "log-service 调用失败"));
            throw new InternalServiceException(code, message);
        }
        Object data = response.get("data");
        if (data instanceof Map<?, ?> map) {
            return new HashMap<>((Map<String, Object>) map);
        }
        return new HashMap<>();
    }
}
