package org.example.userservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("attribute")
public class Attribute {

    @TableId(type = IdType.AUTO)
    private Long attrId;

    private Long userId;

    private String attrKey;         // 属性键名，如 nickname、address

    private String attrValue;       // 属性值
}
