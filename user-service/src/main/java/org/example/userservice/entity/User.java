package org.example.userservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String credentialType;  // PHONE / EMAIL / USERNAME

    private String credential;      // 手机号/邮箱/用户名

    private String passwordHash;

    private LocalDateTime createTime;
}