package org.example.userservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("group_permission")
public class GroupPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;

    private Long permId;
}
