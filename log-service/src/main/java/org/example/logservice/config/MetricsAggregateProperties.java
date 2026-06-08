package org.example.logservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "devops.metrics.aggregate")
public class MetricsAggregateProperties {

    private boolean enabled = true;

    private int windowMinutes = 5;

    private String cron = "0 */5 * * * *";

    /** 启动时补跑最近 N 个完整窗口 */
    private int backfillWindows = 12;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getWindowMinutes() {
        return windowMinutes;
    }

    public void setWindowMinutes(int windowMinutes) {
        this.windowMinutes = windowMinutes;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public int getBackfillWindows() {
        return backfillWindows;
    }

    public void setBackfillWindows(int backfillWindows) {
        this.backfillWindows = backfillWindows;
    }
}
