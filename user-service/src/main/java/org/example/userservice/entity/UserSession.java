package org.example.userservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_session")
public class UserSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String deviceId;

    private String sessionId;

    private String deviceType;      // WEB / ANDROID / IOS / DESKTOP / UNKNOWN

    private String clientIp;

    private String userAgent;

    private LocalDateTime loginAt;

    private LocalDateTime lastActiveAt;

    private String status;          // ACTIVE / TERMINATED / EXPIRED
}
