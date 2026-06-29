package org.example.userservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_permission")
public class UserPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long permId;

    private String status;          // ACTIVE / PENDING / REJECTED

    private LocalDateTime createdAt;
}
