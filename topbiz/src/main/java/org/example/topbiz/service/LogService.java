package org.example.topbiz.service;

import org.example.topbiz.feign.LogServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 日志服务：审计查询、运维日志、指标的编排
 */
@Service
public class LogService {

    @Autowired
    private LogServiceClient logServiceClient;

    public Map<String, Object> queryUserAudit(Long userId, Map<String, Object> params) {
        return logServiceClient.queryUserAudit(userId, params);
    }

    public Map<String, Object> queryOpsLogs(Map<String, Object> params) {
        // TODO: 参数校验（service_name, start_time, end_time 等）
        return logServiceClient.queryOpsLogs(params);
    }

    public Map<String, Object> queryMetrics(Map<String, Object> params) {
        // TODO: 参数校验（metric, start_time, end_time 等）
        return logServiceClient.queryMetrics(params);
    }

    public Map<String, Object> exportLogs(Map<String, Object> request) {
        // TODO: 参数校验（format: csv/json/txt）
        return logServiceClient.exportLogs(request);
    }

    public Map<String, Object> getMetricsConfig() {
        return logServiceClient.getMetricsConfig();
    }

    public Map<String, Object> updateMetricsConfig(Map<String, Object> request) {
        return logServiceClient.updateMetricsConfig(request);
    }
}