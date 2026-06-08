package org.example.logservice.support;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class ClickHouseSchemaSupport {

    private final DataSource clickHouseDataSource;
    private volatile Boolean metricsAggregateAvailable;

    public ClickHouseSchemaSupport(@Qualifier("clickHouseDataSource") DataSource clickHouseDataSource) {
        this.clickHouseDataSource = clickHouseDataSource;
    }

    public boolean isMetricsAggregateAvailable() {
        if (metricsAggregateAvailable == null) {
            metricsAggregateAvailable = checkTableExists("metrics_aggregate");
        }
        return metricsAggregateAvailable;
    }

    public void markMetricsAggregateUnavailable() {
        metricsAggregateAvailable = false;
    }

    public void refreshMetricsAggregateAvailability() {
        metricsAggregateAvailable = checkTableExists("metrics_aggregate");
    }

    private boolean checkTableExists(String tableName) {
        String sql = "SELECT count() FROM system.tables WHERE database = 'devops' AND name = ?";
        try (Connection conn = clickHouseDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }
}
