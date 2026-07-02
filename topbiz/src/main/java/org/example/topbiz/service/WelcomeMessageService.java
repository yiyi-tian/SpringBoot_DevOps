package org.example.topbiz.service;

import org.example.topbiz.config.DevopsMessagingProperties;
import org.example.topbiz.feign.MessageServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 注册/管理员创建用户后的欢迎站内信编排（配置驱动，可复用）
 */
@Service
public class WelcomeMessageService {

    private static final Logger log = LoggerFactory.getLogger(WelcomeMessageService.class);

    private final MessageServiceClient messageServiceClient;
    private final DevopsMessagingProperties messagingProperties;

    @Autowired
    public WelcomeMessageService(MessageServiceClient messageServiceClient,
                                 DevopsMessagingProperties messagingProperties) {
        this.messageServiceClient = messageServiceClient;
        this.messagingProperties = messagingProperties;
    }

    public void sendWelcomeMessage(Long userId) {
        if (userId == null) {
            return;
        }
        DevopsMessagingProperties.Welcome welcome = messagingProperties.getWelcome();
        Map<String, Object> msgRequest = new HashMap<>();
        msgRequest.put("channelType", welcome.getChannelType());
        msgRequest.put("templateId", welcome.getTemplateId());
        msgRequest.put("receiver", String.valueOf(userId));
        msgRequest.put("variables", Map.of());

        try {
            Map<String, Object> result = messageServiceClient.sendInstant(msgRequest);
            if (result == null) {
                log.warn("欢迎信发送无响应: userId={}, templateId={}, channelType={}",
                        userId, welcome.getTemplateId(), welcome.getChannelType());
                return;
            }
            Object code = result.get("code");
            if (!"0".equals(String.valueOf(code))) {
                log.warn("欢迎信发送失败: userId={}, templateId={}, channelType={}, code={}, message={}",
                        userId, welcome.getTemplateId(), welcome.getChannelType(), code, result.get("message"));
            }
        } catch (Exception e) {
            log.warn("欢迎信发送异常: userId={}, templateId={}, channelType={}, error={}",
                    userId, welcome.getTemplateId(), welcome.getChannelType(), e.getMessage());
        }
    }
}
