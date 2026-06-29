package org.example.messageservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("msg_template")
public class MsgTemplate {
    @TableId(type = IdType.AUTO)
    private Long templateId;
    private String name;
    private String content;
    private String channelType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}