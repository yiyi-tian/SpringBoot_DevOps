package org.example.messageservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("msg_variable")
public class MsgVariable {
    @TableId(type = IdType.AUTO)
    private Long variableId;
    private String varKey;
    private String name;
    private String type;
    private Integer required;
    private String defaultValue;
    private String scope;
    private String status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}