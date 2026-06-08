package org.example.logservice.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("metrics_threshold_config")
public class MetricsThresholdConfig {

    @TableId
    private String configKey;

    private Double thresholdValue;

    private String severity;

    private LocalDateTime updatedAt;
}
