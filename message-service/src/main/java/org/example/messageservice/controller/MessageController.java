package org.example.messageservice.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessageController {

    @PostMapping("/internal/message/send")
    public String send(@RequestParam Long userId, @RequestParam String content){

        System.out.println("发送消息：" + content);

        return "message success";
    }
}
