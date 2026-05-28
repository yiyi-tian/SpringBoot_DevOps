package org.example.logservice.controller;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class LogController {

    /**
     * 业务审计日志记录
     */
    @PostMapping("/internal/log/record")
    public Map<String, Object> record(@RequestBody Map<String, Object> request) {

        String traceId = (String) request.get("trace_id");
        Long userId = request.get("user_id") != null ?
                Long.valueOf(String.valueOf(request.get("user_id"))) : null;
        String operation = (String) request.get("operation");
        Boolean success = (Boolean) request.get("success");
        String targetId = (String) request.get("target_id");
        String detail = (String) request.get("detail");

        // 打印日志（后续需要写入 MySQL audit_log 表）
        System.out.println("=== 审计日志 ===");
        System.out.println("  trace_id: " + traceId);
        System.out.println("  user_id: " + userId);
        System.out.println("  operation: " + operation);
        System.out.println("  success: " + success);
        System.out.println("  target_id: " + targetId);
        System.out.println("  detail: " + detail);

        // 返回成功
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "ok");
        return result;
    }
}