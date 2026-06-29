package org.example.userservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("`group`")
public class Group {

    @TableId(type = IdType.AUTO)
    private Long groupId;

    private String name;

    private String description;

    private Long creatorUserId;

    private Integer isAdmin;        // 0-普通组, 1-管理员组

    private Integer isDeleted;      // 逻辑删除 0/1

    private LocalDateTime createdAt;
}
