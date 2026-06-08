package org.example.logservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.common.AccessLogMasker;
import org.example.common.AccessLogProperties;
import org.example.logservice.support.ClickHouseSchemaSupport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.logservice.entity.AuditLog;
import org.example.logservice.entity.MetricsThresholdConfig;
import org.example.logservice.mapper.AuditLogMapper;
import org.example.logservice.mapper.MetricsThresholdConfigMapper;
import org.example.logservice.query.OpsQueryBuilder;
import org.example.logservice.query.OpsQuerySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

@Service
public class LogService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final int EXPORT_MAX_SIZE = 10_000;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Set<String> AGGREGATE_METRICS = Set.of(
            "pv", "qps", "error_rate", "p95", "p99", "success_rate", "api_calls"
    );
    private static final Set<String> ALLOWED_OPERATIONS = Set.of(
            "USER_REGISTER", "USER_LOGIN", "USER_LOGOUT", "USER_DEREGISTER",
            "ADMIN_USER_CREATE", "ADMIN_USER_DELETE", "ADMIN_USER_UPDATE"
    );

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Autowired
    private MetricsThresholdConfigMapper metricsThresholdConfigMapper;

    @Autowired
    @Qualifier("clickHouseDataSource")
    private DataSource clickHouseDataSource;

    @Autowired
    private OpsQueryBuilder opsQueryBuilder;

    @Autowired
    private AccessLogProperties accessLogProperties;

    @Autowired
    private ClickHouseSchemaSupport clickHouseSchemaSupport;

    public Map<String, Object> recordAudit(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        String traceId = request.get("trace_id") != null ? String.valueOf(request.get("trace_id")) : null;
        Long userId = parseLong(request.get("user_id"));
        String operation = request.get("operation") != null ? String.valueOf(request.get("operation")) : null;
        boolean success = parseBoolean(request.get("success"), true);
        String targetId = request.get("target_id") != null ? String.valueOf(request.get("target_id")) : null;
        String detail = request.get("detail") != null ? String.valueOf(request.get("detail")) : null;

        if (operation == null || operation.isEmpty()) {
            result.put("code", 400);
            result.put("message", "operation 不能为空");
            return result;
        }
        if (!ALLOWED_OPERATIONS.contains(operation)) {
            result.put("code", 400);
            result.put("message", "不支持的 operation: " + operation);
            return result;
        }

        if (detail != null && !detail.isBlank()) {
            detail = AccessLogMasker.maskAndTruncate(detail, accessLogProperties.getMaxBodyLength());
        }

        AuditLog auditLog = new AuditLog();
        auditLog.setTraceId(traceId);
        auditLog.setUserId(userId);
        auditLog.setOperation(operation);
        auditLog.setSuccess(success);
        auditLog.setTargetId(targetId);
        auditLog.setDetail(detail);
        auditLog.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(auditLog);

        result.put("code", 0);
        result.put("message", "ok");
        result.put("data", Map.of("log_id", auditLog.getLogId()));
        return result;
    }

    public Map<String, Object> queryUserAudit(Long userId, Map<String, Object> params) {
        int page = parseInt(params.get("page"), 1);
        int size = parseInt(params.get("size"), 20);
        String operation = params.get("operation") != null ? String.valueOf(params.get("operation")) : null;
        LocalDateTime startTime = parseDateTime(params.get("start_time"));
        LocalDateTime endTime = parseDateTime(params.get("end_time"));

        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuditLog::getUserId, userId);
        if (operation != null && !operation.isBlank()) {
            wrapper.eq(AuditLog::getOperation, operation);
        }
        if (startTime != null) {
            wrapper.ge(AuditLog::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(AuditLog::getCreatedAt, endTime);
        }
        wrapper.orderByDesc(AuditLog::getCreatedAt);

        Page<AuditLog> pageResult = auditLogMapper.selectPage(new Page<>(page, size), wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("list", pageResult.getRecords());
        data.put("total", pageResult.getTotal());
        data.put("page", page);
        data.put("size", size);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "ok");
        result.put("data", data);
        return result;
    }

    public Map<String, Object> queryOpsLogs(Map<String, Object> params) {
        int page = parseInt(params.get("page"), 1);
        int size = parseInt(params.get("size"), 20);
        OpsQuerySpec spec = opsQueryBuilder.buildAccessLogQuery(params);

        List<Object> countArgs = spec.getArgs();
        long total = queryCount("SELECT count() FROM devops.access_log" + spec.getWhereClause(), countArgs);

        List<Object> queryArgs = new ArrayList<>(spec.getArgs());
        int offset = (page - 1) * size;
        String sql = "SELECT trace_id, service_name, client_ip, method, uri, cost_ms, http_status, biz_code, " +
                "toUnixTimestamp64Milli(timestamp) AS log_time_ms, req_params, res_body, level " +
                "FROM devops.access_log" + spec.getWhereClause() + spec.getOrderBy() + limitOffsetClause(size, offset);

        List<Map<String, Object>> list = queryRows(sql, queryArgs);
        list.forEach(row -> {
            if (row.containsKey("log_time_ms")) {
                row.put("timestamp", row.remove("log_time_ms"));
            }
        });

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        data.put("start_time", spec.getStartMs());
        data.put("end_time", spec.getEndMs());

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "ok");
        result.put("data", data);
        return result;
    }

    public Map<String, Object> queryMetrics(Map<String, Object> params) {
        Map<String, Object> metricError = validateMetricParam(params);
        if (metricError != null) {
            return metricError;
        }

        String source = params.get("source") != null ? String.valueOf(params.get("source")) : "raw";
        if ("aggregate".equalsIgnoreCase(source)) {
            return queryMetricsFromAggregate(params);
        }
        return queryMetricsFromRaw(params);
    }

    private Map<String, Object> validateMetricParam(Map<String, Object> params) {
        Object metric = params.get("metric");
        if (metric == null || String.valueOf(metric).isBlank()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "metric 参数不能为空");
            return result;
        }
        return null;
    }

    private Map<String, Object> queryMetricsFromRaw(Map<String, Object> params) {
        String metric = String.valueOf(params.get("metric"));
        OpsQuerySpec spec = opsQueryBuilder.buildAccessLogQuery(params);
        long startMs = spec.getStartMs();
        long endMs = spec.getEndMs();
        int topN = parseInt(params.get("top_n"), 10);
        int intervalSec = parseInt(params.get("interval"), 60);
        long slowThreshold = accessLogProperties.getSlowRequestThresholdMs();

        String where = spec.getWhereClause();
        List<Object> args = spec.getArgs();

        Object value = switch (metric) {
            case "qps" -> querySingleDouble(
                    "SELECT count() / greatest(dateDiff('second', fromUnixTimestamp64Milli(?), fromUnixTimestamp64Milli(?)), 1) FROM devops.access_log" + where,
                    prependRangeArgs(args, startMs, endMs));
            case "pv" -> querySingleLong("SELECT count() FROM devops.access_log" + where, args);
            case "api_calls" -> querySingleLong("SELECT count() FROM devops.access_log" + where, args);
            case "slow_count" -> querySingleLong("SELECT countIf(cost_ms > ?) FROM devops.access_log" + where,
                    appendArg(args, slowThreshold));
            case "error_rate" -> querySingleDouble(
                    "SELECT if(count() = 0, 0, countIf(http_status >= 400 OR (biz_code IS NOT NULL AND biz_code != '0')) / count()) FROM devops.access_log" + where,
                    args);
            case "error_count" -> querySingleLong(
                    "SELECT countIf(http_status >= 400 OR (biz_code IS NOT NULL AND biz_code != '0')) FROM devops.access_log" + where,
                    args);
            case "http_5xx" -> querySingleLong("SELECT countIf(http_status >= 500) FROM devops.access_log" + where, args);
            case "http_4xx" -> querySingleLong("SELECT countIf(http_status >= 400 AND http_status < 500) FROM devops.access_log" + where, args);
            case "avg" -> querySingleDouble("SELECT avg(cost_ms) FROM devops.access_log" + where, args);
            case "p95" -> querySingleDouble("SELECT quantile(0.95)(cost_ms) FROM devops.access_log" + where, args);
            case "p99" -> querySingleDouble("SELECT quantile(0.99)(cost_ms) FROM devops.access_log" + where, args);
            case "max" -> querySingleDouble("SELECT max(cost_ms) FROM devops.access_log" + where, args);
            case "success_rate" -> querySingleDouble(
                    "SELECT if(count() = 0, 1, countIf(http_status < 400 AND (biz_code IS NULL OR biz_code = '0')) / count()) FROM devops.access_log" + where,
                    args);
            case "slowest_api" -> queryRows(
                    "SELECT uri, max(cost_ms) AS max_cost_ms FROM devops.access_log" + where + " GROUP BY uri ORDER BY max_cost_ms DESC" + limitClause(topN),
                    args);
            case "ip_request_topn" -> queryRows(
                    "SELECT client_ip, count() AS cnt FROM devops.access_log" + where + " GROUP BY client_ip ORDER BY cnt DESC" + limitClause(topN),
                    args);
            case "ip_error_topn" -> queryRows(
                    "SELECT client_ip, countIf(http_status >= 400 OR (biz_code IS NOT NULL AND biz_code != '0')) AS cnt FROM devops.access_log" + where +
                            " GROUP BY client_ip HAVING cnt > 0 ORDER BY cnt DESC" + limitClause(topN),
                    args);
            default -> throw new IllegalArgumentException("Unsupported metric: " + metric);
        };

        return metricsResult(metric, "raw", value, startMs, endMs, intervalSec);
    }

    private Map<String, Object> queryMetricsFromAggregate(Map<String, Object> params) {
        if (!clickHouseSchemaSupport.isMetricsAggregateAvailable()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 503);
            result.put("message", "metrics_aggregate 表不存在，请先执行 docs/sql/05_clickhouse_metrics_aggregate.sql");
            return result;
        }

        String metric = String.valueOf(params.get("metric"));
        if (!AGGREGATE_METRICS.contains(metric)) {
            throw new IllegalArgumentException("metric " + metric + " 不支持 source=aggregate，请使用 source=raw 或改用 pv/qps/error_rate/p95/p99/success_rate/api_calls");
        }

        OpsQuerySpec spec = opsQueryBuilder.buildAggregateMetricsQuery(params);
        long startMs = spec.getStartMs();
        long endMs = spec.getEndMs();
        int intervalSec = parseInt(params.get("interval"), 60);
        String where = spec.getWhereClause();
        List<Object> args = spec.getArgs();

        Object value = switch (metric) {
            case "pv", "api_calls" -> querySingleLong("SELECT sum(pv) FROM devops.metrics_aggregate" + where, args);
            case "qps" -> querySingleDouble(
                    "SELECT sum(pv) / greatest(dateDiff('second', fromUnixTimestamp64Milli(?), fromUnixTimestamp64Milli(?)), 1) FROM devops.metrics_aggregate" + where,
                    prependRangeArgs(args, startMs, endMs));
            case "error_rate" -> querySingleDouble(
                    "SELECT if(sum(pv) = 0, 0, sum(pv * error_rate) / sum(pv)) FROM devops.metrics_aggregate" + where,
                    args);
            case "p95" -> querySingleDouble(
                    "SELECT if(sum(pv) = 0, 0, sum(toFloat64(p95) * pv) / sum(pv)) FROM devops.metrics_aggregate" + where,
                    args);
            case "p99" -> querySingleDouble(
                    "SELECT if(sum(pv) = 0, 0, sum(toFloat64(p99) * pv) / sum(pv)) FROM devops.metrics_aggregate" + where,
                    args);
            case "success_rate" -> querySingleDouble(
                    "SELECT if(sum(pv) = 0, 1, sum(pv * success_rate) / sum(pv)) FROM devops.metrics_aggregate" + where,
                    args);
            default -> throw new IllegalArgumentException("Unsupported metric: " + metric);
        };

        return metricsResult(metric, "aggregate", value, startMs, endMs, intervalSec);
    }

    private Map<String, Object> metricsResult(String metric, String source, Object value, long startMs, long endMs, int intervalSec) {
        Map<String, Object> data = new HashMap<>();
        data.put("metric", metric);
        data.put("source", source);
        data.put("value", value);
        data.put("start_time", startMs);
        data.put("end_time", endMs);
        if ("qps".equals(metric)) {
            data.put("interval", intervalSec);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "ok");
        result.put("data", data);
        return result;
    }

    public Map<String, Object> exportLogs(Map<String, Object> request) {
        Map<String, Object> exportParams = new HashMap<>(request);
        int size = parseInt(exportParams.get("size"), EXPORT_MAX_SIZE);
        exportParams.put("size", Math.min(size, EXPORT_MAX_SIZE));
        if (!exportParams.containsKey("page")) {
            exportParams.put("page", 1);
        }

        Map<String, Object> queryResult = queryOpsLogs(exportParams);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) queryResult.get("data");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("list");

        String format = request.get("format") != null ? String.valueOf(request.get("format")).toLowerCase() : "csv";
        String content = switch (format) {
            case "json" -> toJsonExport(list);
            case "txt" -> toTxtExport(list);
            default -> toCsvExport(list);
        };

        Map<String, Object> exportData = new HashMap<>();
        exportData.put("format", format);
        exportData.put("content", content);
        exportData.put("count", list.size());
        exportData.put("total", data.get("total"));
        long total = data.get("total") instanceof Number number ? number.longValue() : list.size();
        exportData.put("truncated", total > list.size());

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "ok");
        result.put("data", exportData);
        return result;
    }

    public Map<String, Object> getMetricsConfig() {
        List<MetricsThresholdConfig> configs = metricsThresholdConfigMapper.selectList(null);
        Map<String, Object> data = new LinkedHashMap<>();
        List<Map<String, Object>> configList = new ArrayList<>();
        for (MetricsThresholdConfig config : configs) {
            data.put(config.getConfigKey(), config.getThresholdValue());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("config_key", config.getConfigKey());
            item.put("threshold_value", config.getThresholdValue());
            item.put("severity", config.getSeverity());
            configList.add(item);
        }
        data.put("configs", configList);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "ok");
        result.put("data", data);
        return result;
    }

    public Map<String, Object> updateMetricsConfig(Map<String, Object> request) {
        String configKey = request.get("config_key") != null ? String.valueOf(request.get("config_key")) : null;
        if (configKey == null || configKey.isBlank()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "config_key 不能为空");
            return result;
        }

        MetricsThresholdConfig config = metricsThresholdConfigMapper.selectById(configKey);
        boolean isNew = config == null;
        if (isNew) {
            config = new MetricsThresholdConfig();
            config.setConfigKey(configKey);
            config.setSeverity("NORMAL");
        }
        if (request.get("threshold_value") != null) {
            config.setThresholdValue(Double.valueOf(String.valueOf(request.get("threshold_value"))));
        }
        if (request.get("severity") != null) {
            config.setSeverity(String.valueOf(request.get("severity")));
        }
        config.setUpdatedAt(LocalDateTime.now());

        if (isNew) {
            metricsThresholdConfigMapper.insert(config);
        } else {
            metricsThresholdConfigMapper.updateById(config);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "ok");
        return result;
    }

    private long queryCount(String sql, List<Object> args) {
        try (Connection conn = clickHouseDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindArgs(ps, args);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("ClickHouse count query failed: " + e.getMessage(), e);
        }
    }

    private long querySingleLong(String sql, List<Object> args) {
        try (Connection conn = clickHouseDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindArgs(ps, args);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("ClickHouse query failed: " + e.getMessage(), e);
        }
    }

    private double querySingleDouble(String sql, List<Object> args) {
        try (Connection conn = clickHouseDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindArgs(ps, args);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("ClickHouse query failed: " + e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> queryRows(String sql, List<Object> args) {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = clickHouseDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindArgs(ps, args);
            try (ResultSet rs = ps.executeQuery()) {
                int columnCount = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
                    }
                    rows.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("ClickHouse query failed: " + e.getMessage(), e);
        }
        return rows;
    }

    private void bindArgs(PreparedStatement ps, List<Object> args) throws SQLException {
        for (int i = 0; i < args.size(); i++) {
            ps.setObject(i + 1, args.get(i));
        }
    }

    private List<Object> prependRangeArgs(List<Object> args, long startMs, long endMs) {
        List<Object> merged = new ArrayList<>();
        merged.add(startMs);
        merged.add(endMs);
        merged.addAll(args);
        return merged;
    }

    private List<Object> appendArg(List<Object> args, Object value) {
        List<Object> copy = new ArrayList<>(args);
        copy.add(value);
        return copy;
    }

    private String limitOffsetClause(int size, int offset) {
        int safeSize = Math.max(1, Math.min(size, EXPORT_MAX_SIZE));
        int safeOffset = Math.max(0, offset);
        return " LIMIT " + safeSize + " OFFSET " + safeOffset + " ";
    }

    private String limitClause(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, EXPORT_MAX_SIZE));
        return " LIMIT " + safeLimit + " ";
    }

    private String toCsvExport(List<Map<String, Object>> list) {
        if (list.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add(String.join(",", list.get(0).keySet()));
        for (Map<String, Object> row : list) {
            StringJoiner line = new StringJoiner(",");
            for (Object value : row.values()) {
                line.add(value == null ? "" : String.valueOf(value).replace(",", ";"));
            }
            joiner.add(line.toString());
        }
        return joiner.toString();
    }

    private String toTxtExport(List<Map<String, Object>> list) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> row : list) {
            sb.append(row).append(System.lineSeparator());
        }
        return sb.toString();
    }

    private String toJsonExport(List<Map<String, Object>> list) {
        try {
            return OBJECT_MAPPER.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON export failed: " + e.getMessage(), e);
        }
    }

    private boolean parseBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = String.valueOf(value);
        if ("true".equalsIgnoreCase(text) || "1".equals(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text) || "0".equals(text)) {
            return false;
        }
        return defaultValue;
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        return Long.valueOf(String.valueOf(value));
    }

    private int parseInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(number.longValue()), ZONE);
        }
        String text = String.valueOf(value);
        if (text.matches("\\d+")) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(text)), ZONE);
        }
        return LocalDateTime.parse(text);
    }
}
