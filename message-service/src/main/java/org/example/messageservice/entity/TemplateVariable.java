package org.example.messageservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("template_variable")
public class TemplateVariable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private Long variableId;
    private Integer requiredOverride;
    private String defaultOverride;
    private LocalDateTime createdAt;
}