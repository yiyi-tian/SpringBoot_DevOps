CREATE DATABASE IF NOT EXISTS devops;

CREATE TABLE IF NOT EXISTS devops.access_log
(
    trace_id     String,
    service_name LowCardinality(String),
    client_ip    String,
    method       String,
    uri          String,
    cost_ms      UInt32,
    http_status  UInt16,
    biz_code     Nullable(String),
    timestamp    DateTime64(3, 'Asia/Shanghai'),
    req_params   Nullable(String),
    res_body     Nullable(String),
    level        LowCardinality(Nullable(String))
)
ENGINE = MergeTree()
PARTITION BY toYYYYMM(timestamp)
ORDER BY (service_name, timestamp, trace_id)
TTL toDateTime(timestamp) + INTERVAL 90 DAY
SETTINGS index_granularity = 8192;
