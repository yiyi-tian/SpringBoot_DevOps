package org.example.logservice.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LogController {

    @PostMapping("/internal/log/record")
    public String record(
            @RequestParam Long userId,
            @RequestParam String operation
    ) {

        System.out.println("记录日志：" + operation);

        return "log success";
    }
}
