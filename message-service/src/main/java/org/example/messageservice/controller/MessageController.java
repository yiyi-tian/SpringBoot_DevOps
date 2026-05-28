package org.example.messageservice.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class MessageController {

    /**
     * 即时消息发送
     */
    @PostMapping("/internal/messages/instant")
    public Map<String, Object> sendInstant(@RequestBody Map<String, Object> request) {

        String channelType = (String) request.get("channelType");
        Integer templateId = (Integer) request.get("templateId");
        String receiver = (String) request.get("receiver");
        Map<String, Object> variables = (Map<String, Object>) request.get("variables");

        // 打印日志
        System.out.println("发送即时消息：");
        System.out.println("  channelType: " + channelType);
        System.out.println("  templateId: " + templateId);
        System.out.println("  receiver: " + receiver);
        System.out.println("  variables: " + variables);

        // 返回成功
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("messageId", System.currentTimeMillis());
        result.put("data", data);
        return result;
    }
}
