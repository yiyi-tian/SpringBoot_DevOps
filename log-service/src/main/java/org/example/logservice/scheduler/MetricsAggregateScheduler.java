package org.example.logservice.scheduler;

import org.example.logservice.config.MetricsAggregateProperties;
import org.example.logservice.support.ClickHouseSchemaSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class MetricsAggregateScheduler {

    private static final Logger log = LoggerFactory.getLogger(MetricsAggregateScheduler.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private static final String AGGREGATE_SQL = """
            INSERT INTO devops.metrics_aggregate
            SELECT
              toStartOfFiveMinutes(timestamp) AS window_start,
              service_name,
              uri,
              count() AS pv,
              count() / 300.0 AS qps,
              if(count() = 0, 0, countIf(http_status >= 400 OR (biz_code IS NOT NULL AND biz_code != '0')) / count()) AS error_rate,
              quantile(0.95)(cost_ms) AS p95,
              quantile(0.99)(cost_ms) AS p99,
              if(count() = 0, 1, countIf(http_status < 400 AND (biz_code IS NULL OR biz_code = '0')) / count()) AS success_rate,
              NULL AS ip_risk_score
            FROM devops.access_log
            WHERE timestamp >= fromUnixTimestamp64Milli(?) AND timestamp < fromUnixTimestamp64Milli(?)
            GROUP BY window_start, service_name, uri
            """;

    @Autowired
    private MetricsAggregateProperties properties;

    @Autowired
    @Qualifier("clickHouseDataSource")
    private DataSource clickHouseDataSource;

    @Autowired
    private ClickHouseSchemaSupport clickHouseSchemaSupport;

    @EventListener(ApplicationReadyEvent.class)
    public void backfillOnStartup() {
        if (!properties.isEnabled()) {
            return;
        }
        if (!clickHouseSchemaSupport.isMetricsAggregateAvailable()) {
            log.warn("devops.metrics_aggregate 表不存在，已跳过预聚合调度；请执行 docs/sql/05_clickhouse_metrics_aggregate.sql");
            properties.setEnabled(false);
            return;
        }
        if (properties.getBackfillWindows() <= 0) {
            return;
        }
        long windowMs = properties.getWindowMinutes() * 60_000L;
        WindowRange lastComplete = previousCompleteWindow(System.currentTimeMillis(), properties.getWindowMinutes());
        for (int i = properties.getBackfillWindows(); i >= 1; i--) {
            long windowEnd = lastComplete.endMs() - (i - 1) * windowMs;
            long windowStart = windowEnd - windowMs;
            aggregateWindow(windowStart, windowEnd);
        }
    }

    @Scheduled(cron = "${devops.metrics.aggregate.cron:0 */5 * * * *}")
    public void aggregateScheduled() {
        if (!properties.isEnabled()) {
            return;
        }
        WindowRange window = previousCompleteWindow(System.currentTimeMillis(), properties.getWindowMinutes());
        aggregateWindow(window.startMs(), window.endMs());
    }

    public void aggregateWindow(long windowStartMs, long windowEndMs) {
        try (Connection conn = clickHouseDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(AGGREGATE_SQL)) {
            ps.setLong(1, windowStartMs);
            ps.setLong(2, windowEndMs);
            ps.execute();
            log.info("Aggregated access_log window [{} - {})", Instant.ofEpochMilli(windowStartMs), Instant.ofEpochMilli(windowEndMs));
        } catch (SQLException e) {
            if (isUnknownTable(e)) {
                clickHouseSchemaSupport.markMetricsAggregateUnavailable();
                properties.setEnabled(false);
                log.warn("devops.metrics_aggregate 表不存在，已禁用预聚合调度；请执行 docs/sql/05_clickhouse_metrics_aggregate.sql");
                return;
            }
            log.error("Metrics aggregate failed for window [{} - {}): {}", windowStartMs, windowEndMs, e.getMessage(), e);
        }
    }

    private boolean isUnknownTable(SQLException e) {
        String message = e.getMessage();
        return message != null && message.contains("UNKNOWN_TABLE");
    }

    private WindowRange previousCompleteWindow(long nowMs, int windowMinutes) {
        ZonedDateTime now = Instant.ofEpochMilli(nowMs).atZone(ZONE);
        int alignedMinute = (now.getMinute() / windowMinutes) * windowMinutes;
        ZonedDateTime currentSlotStart = now.withMinute(alignedMinute).withSecond(0).withNano(0);
        ZonedDateTime windowEnd = currentSlotStart;
        ZonedDateTime windowStart = windowEnd.minusMinutes(windowMinutes);
        return new WindowRange(windowStart.toInstant().toEpochMilli(), windowEnd.toInstant().toEpochMilli());
    }

    private record WindowRange(long startMs, long endMs) {
    }
}
