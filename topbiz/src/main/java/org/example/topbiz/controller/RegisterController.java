package org.example.topbiz.controller;

import org.example.common.Result;
import org.example.topbiz.feign.UserServiceClient;
import org.example.topbiz.feign.MessageServiceClient;
import org.example.topbiz.feign.LogServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
public class RegisterController {

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private MessageServiceClient messageServiceClient;

    @Autowired
    private LogServiceClient logServiceClient;

    // 手机号正则：1开头的11位数字
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[0-9]{10}$");
    // 邮箱正则
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @PostMapping("/api/v1/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, Object> request) {

        // 获取凭证（支持 credential / phone / email / username 字段）
        String credential = (String) request.getOrDefault("credential",
                request.getOrDefault("phone",
                        request.getOrDefault("email",
                                request.get("username"))));

        String password = (String) request.get("password");

        // 参数校验
        if (credential == null || credential.isEmpty()) {
            return Result.error(400, "请输入手机号/邮箱/用户名");
        }
        if (password == null || password.length() < 6) {
            return Result.error(400, "密码长度不能少于6位");
        }

        // 自动识别凭证类型
        String credentialType = detectCredentialType(credential);

        // 构造给 user-service 的请求
        Map<String, Object> userRequest = new HashMap<>();
        userRequest.put("credentialType", credentialType);
        userRequest.put("credential", credential);
        userRequest.put("password", password);

        // 1. 调用 user-service 注册
        Map<String, Object> userResult = userServiceClient.register(userRequest);

        if (userResult == null || !"0".equals(String.valueOf(userResult.get("code")))) {
            String msg = userResult != null ?
                    String.valueOf(userResult.get("message")) : "注册失败";
            return Result.error(500, msg);
        }

        Map<String, Object> userData = (Map<String, Object>) userResult.get("data");
        Long userId = Long.valueOf(String.valueOf(userData.get("userId")));

        // 2. 发送欢迎站内信
        try {
            Map<String, Object> msgRequest = new HashMap<>();
            msgRequest.put("channelType", "IN_APP");
            msgRequest.put("templateId", 1);
            msgRequest.put("receiver", String.valueOf(userId));
            msgRequest.put("variables", new HashMap<>());
            messageServiceClient.sendInstant(msgRequest);
        } catch (Exception e) {
            System.err.println("发送欢迎信失败: " + e.getMessage());
        }

        // 3. 记录审计日志
        try {
            Map<String, Object> logRequest = new HashMap<>();
            logRequest.put("trace_id", getTraceId());
            logRequest.put("user_id", userId);
            logRequest.put("operation", "USER_REGISTER");
            logRequest.put("success", true);
            logRequest.put("target_id", String.valueOf(userId));
            logServiceClient.recordAudit(logRequest);
        } catch (Exception e) {
            System.err.println("记录审计日志失败: " + e.getMessage());
        }

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("userId", userId);
        resultData.put("credentialType", credentialType);
        return Result.ok(resultData);
    }

    /**
     * 自动识别凭证类型
     */
    private String detectCredentialType(String credential) {
        if (PHONE_PATTERN.matcher(credential).matches()) {
            return "PHONE";
        }
        if (EMAIL_PATTERN.matcher(credential).matches()) {
            return "EMAIL";
        }
        return "USERNAME";
    }

    private String getTraceId() {
        String traceId = org.slf4j.MDC.get("traceId");
        return traceId != null ? traceId : UUID.randomUUID().toString();
    }
}