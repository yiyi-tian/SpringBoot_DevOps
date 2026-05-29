package org.example.userservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_auth")
public class UserAuth {

    @TableId(type = IdType.AUTO)
    private Long authId;

    private Long userId;            // 关联 user.id

    private String identityType;    // PHONE / EMAIL / USERNAME / FEISHU / WECHAT

    private String identifier;      // 手机号/邮箱/用户名/openid

    private String secretHash;      // BCrypt 密码哈希（OAuth 可空）

    private Integer verified;       // 是否已验证 0/1

    private LocalDateTime createdAt;
}