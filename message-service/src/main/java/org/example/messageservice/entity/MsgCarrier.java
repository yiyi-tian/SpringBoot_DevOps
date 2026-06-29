package org.example.messageservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("msg_carrier")
public class MsgCarrier {
    @TableId(type = IdType.AUTO)
    private Long carrierId;
    private String name;
    private String provider;
    private String channelType;
    private String configJson;
    private Integer enabled;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
