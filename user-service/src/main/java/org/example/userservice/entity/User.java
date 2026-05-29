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
    private Long id;

    private String displayName;     // 显示名（默认用手机号/邮箱/用户名）

    private Integer sex;            // 可选，性别

    private String status;          // ACTIVE / LOCKED / DEREGISTERED

    private Integer isDeleted;      // 逻辑删除 0/1

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}