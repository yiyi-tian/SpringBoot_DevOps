package org.example.userservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("login_history")
public class LoginHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String clientIp;

    private String userAgent;

    private String sessionId;

    private LocalDateTime loginAt;
}
