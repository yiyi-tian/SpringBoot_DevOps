CREATE TABLE IF NOT EXISTS devops.metrics_aggregate
(
    window_start   DateTime('Asia/Shanghai'),
    service_name   LowCardinality(String),
    uri            String,
    pv             UInt64,
    qps            Float64,
    error_rate     Float64,
    p95            UInt32,
    p99            UInt32,
    success_rate   Float64,
    ip_risk_score  Nullable(Float64)
)
ENGINE = ReplacingMergeTree()
PARTITION BY toYYYYMM(window_start)
ORDER BY (service_name, uri, window_start)
TTL toDateTime(window_start) + INTERVAL 90 DAY
SETTINGS index_granularity = 8192;
