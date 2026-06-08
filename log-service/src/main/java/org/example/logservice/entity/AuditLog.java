package org.example.logservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_log")
public class AuditLog {

    @TableId(value = "log_id", type = IdType.AUTO)
    @JsonProperty("log_id")
    private Long logId;

    @TableField("trace_id")
    @JsonProperty("trace_id")
    private String traceId;

    @TableField("user_id")
    @JsonProperty("user_id")
    private Long userId;

    private String operation;

    private Boolean success;

    @TableField("target_id")
    @JsonProperty("target_id")
    private String targetId;

    private String detail;

    @TableField("created_at")
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
