package org.example.messageservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("msg_message")
public class MsgMessage {

    @TableId(type = IdType.AUTO)
    private Long messageId;

    private Long taskId;
    private Long templateId;
    private Long carrierId;
    private String receiver;
    private String renderedContent;
    private String status;
    private String providerMsgId;
    private LocalDateTime sendTime;
    private String errorMessage;
    private LocalDateTime createdAt;
}