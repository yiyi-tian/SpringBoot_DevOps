package org.example.userservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("permission")
public class Permission {

    @TableId(type = IdType.AUTO)
    private Long permId;

    private String permCode;        // 权限编码，如 user:add

    private String permName;        // 权限名称

    private Integer active;         // 0-禁用, 1-启用
}
