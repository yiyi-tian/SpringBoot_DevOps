package org.example.userservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long userId;

    private String displayName;     // 显示名（默认用手机号/邮箱/用户名）

    private Integer sex;            // 性别（可选）

    private String status;          // ACTIVE / LOCKED / DEREGISTERED / EXPIRED / INACTIVE

    private Integer isDeleted;      // 逻辑删除 0/1

    private LocalDateTime lastLoginAt;   // 最后登录时间，用于活跃度检测

    private String lastLoginIp;          // 最后登录IP（v4/v6），用于IP突变检测

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}