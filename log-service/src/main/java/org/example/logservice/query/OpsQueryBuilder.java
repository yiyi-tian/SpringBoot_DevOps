package org.example.logservice.query;

import org.example.common.AccessLogProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class OpsQueryBuilder {

    private static final Set<String> SORT_FIELDS = Set.of("timestamp", "cost_ms", "http_status");
    private static final long DEFAULT_RANGE_MS = 86_400_000L;

    @Autowired
    private AccessLogProperties accessLogProperties;

    public Map<String, Object> normalize(Map<String, Object> raw) {
        Map<String, Object> params = new HashMap<>(raw);
        flattenNested(params, "filters");
        if (params.get("sort") instanceof Map<?, ?> sort) {
            if (sort.get("field") != null) {
                params.put("sort_by", sort.get("field"));
            }
            if (sort.get("order") != null) {
                params.put("sort_order", sort.get("order"));
            }
        }
        if (params.get("uri_prefix") != null && params.get("api") == null) {
            params.put("api", params.get("uri_prefix"));
        }
        return params;
    }

    public OpsQuerySpec buildAccessLogQuery(Map<String, Object> rawParams) {
        Map<String, Object> params = normalize(rawParams);
        long endMs = resolveEndMs(params);
        long startMs = resolveStartMs(params, endMs);

        StringBuilder where = new StringBuilder(
                " WHERE timestamp >= fromUnixTimestamp64Milli(?) AND timestamp <= fromUnixTimestamp64Milli(?) ");
        List<Object> args = new ArrayList<>();
        args.add(startMs);
        args.add(endMs);

        appendInFilter(where, args, params, "service_names", "service_name");
        appendSingleOrIn(where, args, params, "service_name", "service_names", "service_name");

        appendInFilter(where, args, params, "trace_ids", "trace_id");
        appendSingleOrIn(where, args, params, "trace_id", "trace_ids", "trace_id");

        appendInFilter(where, args, params, "uris", "uri");
        appendSingleOrIn(where, args, params, "uri", "uris", "uri");

        appendInFilter(where, args, params, "methods", "method");
        appendSingleOrIn(where, args, params, "method", "methods", "method");

        appendInFilter(where, args, params, "levels", "level");
        appendSingleOrIn(where, args, params, "level", "levels", "level");

        appendInFilter(where, args, params, "client_ips", "client_ip");
        appendSingleOrIn(where, args, params, "client_ip", "client_ips", "client_ip");

        appendInFilter(where, args, params, "biz_codes", "biz_code");
        appendSingleOrIn(where, args, params, "biz_code", "biz_codes", "biz_code");

        appendLikePrefix(where, args, params, "api", "uri");

        appendExact(where, args, params, "http_status", "http_status = ?");
        appendRange(where, args, params, "http_status_min", "http_status >= ?");
        appendRange(where, args, params, "http_status_max", "http_status <= ?");
        appendRange(where, args, params, "cost_ms_min", "cost_ms >= ?");
        appendRange(where, args, params, "cost_ms_max", "cost_ms <= ?");

        if (isTrue(params.get("slow_only"))) {
            where.append(" AND cost_ms > ? ");
            args.add(accessLogProperties.getSlowRequestThresholdMs());
        }
        if (isTrue(params.get("has_error"))) {
            where.append(" AND (http_status >= 400 OR (biz_code IS NOT NULL AND biz_code != '0')) ");
        }
        if (params.get("keyword") != null && !String.valueOf(params.get("keyword")).isBlank()) {
            where.append(" AND (uri LIKE ? OR req_params LIKE ? OR res_body LIKE ?) ");
            String kw = "%" + params.get("keyword") + "%";
            args.add(kw);
            args.add(kw);
            args.add(kw);
        }

        String orderBy = buildOrderBy(params);
        return new OpsQuerySpec(where.toString(), args, orderBy, startMs, endMs);
    }

    public OpsQuerySpec buildAggregateMetricsQuery(Map<String, Object> rawParams) {
        Map<String, Object> params = normalize(rawParams);
        long endMs = resolveEndMs(params);
        long startMs = resolveStartMs(params, endMs);

        StringBuilder where = new StringBuilder(
                " WHERE window_start >= fromUnixTimestamp64Milli(?) AND window_start <= fromUnixTimestamp64Milli(?) ");
        List<Object> args = new ArrayList<>();
        args.add(startMs);
        args.add(endMs);

        appendSingleOrIn(where, args, params, "service_name", "service_names", "service_name");
        appendLikePrefix(where, args, params, "api", "uri");

        return new OpsQuerySpec(where.toString(), args, "", startMs, endMs);
    }

    private long resolveEndMs(Map<String, Object> params) {
        if (params.containsKey("end_time") && params.get("end_time") != null) {
            return parseEpochMs(params.get("end_time"), System.currentTimeMillis());
        }
        return System.currentTimeMillis();
    }

    private long resolveStartMs(Map<String, Object> params, long endMs) {
        if (params.containsKey("start_time") && params.get("start_time") != null) {
            return parseEpochMs(params.get("start_time"), endMs - DEFAULT_RANGE_MS);
        }
        if (params.get("time_range") != null && !String.valueOf(params.get("time_range")).isBlank()) {
            return endMs - parseTimeRangeMs(String.valueOf(params.get("time_range")));
        }
        return endMs - DEFAULT_RANGE_MS;
    }

    private long parseTimeRangeMs(String timeRange) {
        return switch (timeRange.toLowerCase()) {
            case "1h" -> 3_600_000L;
            case "24h" -> 86_400_000L;
            case "7d" -> 7 * 86_400_000L;
            case "30d" -> 30 * 86_400_000L;
            default -> DEFAULT_RANGE_MS;
        };
    }

    private String buildOrderBy(Map<String, Object> params) {
        String sortBy = params.get("sort_by") != null ? String.valueOf(params.get("sort_by")) : "timestamp";
        if (!SORT_FIELDS.contains(sortBy)) {
            sortBy = "timestamp";
        }
        String sortOrder = params.get("sort_order") != null ? String.valueOf(params.get("sort_order")).toLowerCase() : "desc";
        if (!"asc".equals(sortOrder) && !"desc".equals(sortOrder)) {
            sortOrder = "desc";
        }
        return " ORDER BY " + sortBy + " " + sortOrder.toUpperCase() + " ";
    }

    @SuppressWarnings("unchecked")
    private void flattenNested(Map<String, Object> params, String key) {
        Object nested = params.get(key);
        if (nested instanceof Map<?, ?> map) {
            map.forEach((k, v) -> params.put(String.valueOf(k), v));
        }
    }

    private void appendSingleOrIn(StringBuilder where, List<Object> args, Map<String, Object> params,
                                  String singleKey, String listKey, String column) {
        if (hasList(params, listKey)) {
            return;
        }
        appendExact(where, args, params, singleKey, column + " = ?");
    }

    private void appendExact(StringBuilder where, List<Object> args, Map<String, Object> params, String key, String clause) {
        if (params.get(key) != null && !String.valueOf(params.get(key)).isBlank()) {
            where.append(" AND ").append(clause).append(" ");
            args.add(parseFilterValue(params.get(key)));
        }
    }

    private void appendRange(StringBuilder where, List<Object> args, Map<String, Object> params, String key, String clause) {
        if (params.get(key) != null && !String.valueOf(params.get(key)).isBlank()) {
            where.append(" AND ").append(clause).append(" ");
            args.add(parseFilterValue(params.get(key)));
        }
    }

    private void appendLikePrefix(StringBuilder where, List<Object> args, Map<String, Object> params, String key, String column) {
        if (params.get(key) != null && !String.valueOf(params.get(key)).isBlank()) {
            where.append(" AND ").append(column).append(" LIKE ? ");
            args.add(String.valueOf(params.get(key)) + "%");
        }
    }

    private void appendInFilter(StringBuilder where, List<Object> args, Map<String, Object> params, String listKey, String column) {
        if (!hasList(params, listKey)) {
            return;
        }
        List<?> values = toList(params.get(listKey));
        if (values.isEmpty()) {
            return;
        }
        where.append(" AND ").append(column).append(" IN (");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                where.append(", ");
            }
            where.append("?");
            args.add(String.valueOf(values.get(i)));
        }
        where.append(") ");
    }

    private boolean hasList(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value instanceof Collection<?> collection && !collection.isEmpty();
    }

    private List<?> toList(Object value) {
        if (value instanceof Collection<?> collection) {
            return new ArrayList<>(collection);
        }
        return List.of(value);
    }

    private Object parseFilterValue(Object value) {
        if (value instanceof Number number) {
            return number;
        }
        String text = String.valueOf(value);
        if (text.matches("-?\\d+")) {
            return Long.parseLong(text);
        }
        if (text.matches("-?\\d+\\.\\d+")) {
            return Double.parseDouble(text);
        }
        return text;
    }

    private boolean isTrue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return "true".equalsIgnoreCase(String.valueOf(value));
    }

    private long parseEpochMs(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value);
        if (text.matches("\\d+")) {
            return Long.parseLong(text);
        }
        return Instant.parse(text).toEpochMilli();
    }
}
