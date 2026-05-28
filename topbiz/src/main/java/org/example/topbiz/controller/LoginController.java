package org.example.topbiz.controller;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.example.common.Result;
import org.example.topbiz.feign.UserServiceClient;
import org.example.topbiz.feign.LogServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
public class LoginController {

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private LogServiceClient logServiceClient;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[0-9]{10}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @PostMapping("/api/v1/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, Object> request) {

        String credential = (String) request.getOrDefault("credential",
                request.getOrDefault("phone",
                        request.getOrDefault("email",
                                request.get("username"))));

        String password = (String) request.get("password");

        if (credential == null || credential.isEmpty()) {
            return Result.error(400, "请输入手机号/邮箱/用户名");
        }
        if (password == null || password.isEmpty()) {
            return Result.error(400, "请输入密码");
        }

        String credentialType = detectCredentialType(credential);

        // 调用 user-service 登录
        Map<String, Object> userRequest = new HashMap<>();
        userRequest.put("credentialType", credentialType);
        userRequest.put("credential", credential);
        userRequest.put("password", password);

        Map<String, Object> userResult = userServiceClient.login(userRequest);

        if (userResult == null || !"0".equals(String.valueOf(userResult.get("code")))) {
            String msg = userResult != null ?
                    String.valueOf(userResult.get("message")) : "登录失败";
            return Result.error(401, msg);
        }

        Map<String, Object> userData = (Map<String, Object>) userResult.get("data");
        Long userId = Long.valueOf(String.valueOf(userData.get("userId")));

        // 创建 Shiro 会话
        Subject subject = SecurityUtils.getSubject();
        subject.login(new UsernamePasswordToken(
                String.valueOf(userId),
                "NOT_USED"
        ));

        // 记录审计日志
        try {
            Map<String, Object> logRequest = new HashMap<>();
            logRequest.put("trace_id", getTraceId());
            logRequest.put("user_id", userId);
            logRequest.put("operation", "USER_LOGIN");
            logRequest.put("success", true);
            logServiceClient.recordAudit(logRequest);
        } catch (Exception e) {
            System.err.println("记录审计日志失败: " + e.getMessage());
        }

        return Result.ok(userData);
    }

    private String detectCredentialType(String credential) {
        if (PHONE_PATTERN.matcher(credential).matches()) return "PHONE";
        if (EMAIL_PATTERN.matcher(credential).matches()) return "EMAIL";
        return "USERNAME";
    }

    private String getTraceId() {
        String traceId = org.slf4j.MDC.get("traceId");
        return traceId != null ? traceId : UUID.randomUUID().toString();
    }
}