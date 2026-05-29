package org.example.topbiz.service;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.example.topbiz.feign.LogServiceClient;
import org.example.topbiz.feign.MessageServiceClient;
import org.example.topbiz.feign.UserServiceClient;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 认证服务：注册、登录、验证码
 */
@Service
public class AuthService {

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private MessageServiceClient messageServiceClient;

    @Autowired
    private LogServiceClient logServiceClient;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[0-9]{10}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * 注册（凭证+密码 / 验证码）
     */
    public Map<String, Object> register(String credential, String password, String code) {
        String credentialType = detectCredentialType(credential);

        // TODO: 如果是验证码注册（code != null），先调用 message-service 校验验证码
        if (code != null) {
            // verifyCode(credentialType, credential, "REGISTER", code);
        }

        // 调用 user-service 注册
        Map<String, Object> userRequest = new HashMap<>();
        userRequest.put("credentialType", credentialType);
        userRequest.put("credential", credential);
        userRequest.put("password", password);

        Map<String, Object> userResult = userServiceClient.register(userRequest);

        if (userResult == null || !"0".equals(String.valueOf(userResult.get("code")))) {
            return userResult;
        }

        Map<String, Object> userData = (Map<String, Object>) userResult.get("data");
        Long userId = Long.valueOf(String.valueOf(userData.get("userId")));

        // 发送欢迎信（失败不影响注册）
        sendWelcomeMessage(userId);

        // 记录审计日志（失败不影响注册）
        recordAudit(userId, "USER_REGISTER", String.valueOf(userId));

        return userResult;
    }

    /**
     * 登录（凭证+密码 / 验证码）
     */
    public Map<String, Object> login(String credential, String password, String code) {
        String credentialType = detectCredentialType(credential);

        // TODO: 如果是验证码登录（code != null），先校验验证码
        if (code != null) {
            // verifyCode(credentialType, credential, "LOGIN", code);
        }

        // 调用 user-service 登录
        Map<String, Object> userRequest = new HashMap<>();
        userRequest.put("credentialType", credentialType);
        userRequest.put("credential", credential);
        userRequest.put("password", password);

        Map<String, Object> userResult = userServiceClient.login(userRequest);

        if (userResult == null || !"0".equals(String.valueOf(userResult.get("code")))) {
            return userResult;
        }

        Map<String, Object> userData = (Map<String, Object>) userResult.get("data");
        Long userId = Long.valueOf(String.valueOf(userData.get("userId")));

        // 创建 Shiro 会话
        Subject subject = SecurityUtils.getSubject();
        subject.login(new UsernamePasswordToken(String.valueOf(userId), "NOT_USED"));

        // 记录审计日志
        recordAudit(userId, "USER_LOGIN", null);

        return userResult;
    }

    /**
     * 发送注册邮箱验证码
     */
    public void sendRegisterEmailCode(String email) {
        Map<String, Object> msgRequest = new HashMap<>();
        msgRequest.put("email", email);
        msgRequest.put("scene", "REGISTER");
        messageServiceClient.sendEmailCode(msgRequest);
    }

    /**
     * 发送注册手机验证码
     */
    public void sendRegisterPhoneCode(String phone) {
        Map<String, Object> msgRequest = new HashMap<>();
        msgRequest.put("phone", phone);
        msgRequest.put("scene", "REGISTER");
        messageServiceClient.sendPhoneCode(msgRequest);
    }

    /**
     * 发送登录邮箱验证码
     */
    public void sendLoginEmailCode(String email) {
        Map<String, Object> msgRequest = new HashMap<>();
        msgRequest.put("email", email);
        msgRequest.put("scene", "LOGIN");
        messageServiceClient.sendEmailCode(msgRequest);
    }

    /**
     * 发送登录手机验证码
     */
    public void sendLoginPhoneCode(String phone) {
        Map<String, Object> msgRequest = new HashMap<>();
        msgRequest.put("phone", phone);
        msgRequest.put("scene", "LOGIN");
        messageServiceClient.sendPhoneCode(msgRequest);
    }

    /**
     * 校验验证码
     */
    private boolean verifyCode(String credentialType, String target, String scene, String code) {
        // TODO: 调用 message-service 校验验证码
        // Map<String, Object> request = new HashMap<>();
        // request.put("credentialType", credentialType);
        // request.put("target", target);
        // request.put("scene", scene);
        // request.put("code", code);
        // Map<String, Object> result = messageServiceClient.verifyCode(request);
        // return "0".equals(String.valueOf(result.get("code")));
        return true;
    }

    /**
     * 自动识别凭证类型
     */
    public String detectCredentialType(String credential) {
        if (PHONE_PATTERN.matcher(credential).matches()) return "PHONE";
        if (EMAIL_PATTERN.matcher(credential).matches()) return "EMAIL";
        return "USERNAME";
    }

    /**
     * 提取凭证字段（兼容 phone/email/username/credential）
     */
    public String extractCredential(Map<String, Object> request) {
        return (String) request.getOrDefault("credential",
                request.getOrDefault("phone",
                        request.getOrDefault("email",
                                request.get("username"))));
    }

    /**
     * 发送欢迎站内信
     */
    private void sendWelcomeMessage(Long userId) {
        try {
            Map<String, Object> msgRequest = new HashMap<>();
            msgRequest.put("channelType", "IN_APP");
            msgRequest.put("templateId", 1);
            msgRequest.put("receiver", String.valueOf(userId));
            msgRequest.put("variables", new HashMap<>());
            messageServiceClient.sendInstant(msgRequest);
        } catch (Exception e) {
            // TODO: 记录失败日志
        }
    }

    /**
     * 记录审计日志
     */
    private void recordAudit(Long userId, String operation, String targetId) {
        try {
            Map<String, Object> logRequest = new HashMap<>();
            logRequest.put("trace_id", MDC.get("traceId"));
            logRequest.put("user_id", userId);
            logRequest.put("operation", operation);
            logRequest.put("success", true);
            logRequest.put("target_id", targetId);
            logServiceClient.recordAudit(logRequest);
        } catch (Exception e) {
            // TODO: 记录失败日志
        }
    }
}