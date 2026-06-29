package org.example.messageservice.service;

import org.example.common.auth.CredentialValidator;
import org.example.common.auth.EmailVerifyKeys;
import org.example.common.message.MessageConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Service
public class VerificationCodeService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration RATE_LIMIT_TTL = Duration.ofSeconds(60);
    private static final Set<String> VALID_SCENES = MessageConstants.VERIFY_SCENES;

    private final StringRedisTemplate redisTemplate;
    private final EmailSendService emailSendService;
    private final Random random = new Random();

    @Autowired
    public VerificationCodeService(StringRedisTemplate redisTemplate, EmailSendService emailSendService) {
        this.redisTemplate = redisTemplate;
        this.emailSendService = emailSendService;
    }

    public Map<String, Object> sendEmailCode(String email, String scene) {
        return sendCode("email", email, scene, true);
    }

    public Map<String, Object> sendPhoneCode(String phone, String scene) {
        Map<String, Object> result = sendCode("phone", phone, scene, false);
        if ("0".equals(String.valueOf(result.get("code")))) {
            Map<String, Object> data = new HashMap<>();
            data.put("smsProvider", "NOT_CONNECTED");
            data.put("hint", "未接入第三方短信平台，验证码仅输出至 message-service 控制台日志");
            result.put("data", data);
        }
        return result;
    }

    public Map<String, Object> verify(String credentialType, String target, String scene, String code) {
        Map<String, Object> result = new HashMap<>();
        if (target == null || target.isBlank() || scene == null || scene.isBlank() || code == null || code.isBlank()) {
            result.put("code", 400);
            result.put("message", "参数不完整");
            return result;
        }
        if (!VALID_SCENES.contains(scene)) {
            result.put("code", 400);
            result.put("message", "无效的场景");
            return result;
        }

        String channel = channelFromCredentialType(credentialType, target);
        String key = codeKey(channel, scene, target);
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null || !stored.equals(code)) {
            result.put("code", 400);
            result.put("message", "验证码错误或已过期");
            return result;
        }

        redisTemplate.delete(key);

        if ("email".equals(channel)) {
            String normalizedEmail = CredentialValidator.normalizeEmail(target);
            String verifiedKey = EmailVerifyKeys.verifiedKey(scene, normalizedEmail);
            redisTemplate.opsForValue().set(verifiedKey, "1", EmailVerifyKeys.VERIFIED_TTL);
        }

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("valid", true);
        result.put("data", data);
        return result;
    }

    public Map<String, Object> sendRegistrationConfirmEmail(String email) {
        Map<String, Object> result = new HashMap<>();
        if (email == null || email.isBlank()) {
            result.put("code", 400);
            result.put("message", "email 不能为空");
            return result;
        }
        String normalized = CredentialValidator.normalizeEmail(email);
        if (CredentialValidator.validate(org.example.common.auth.CredentialType.EMAIL, normalized).isPresent()) {
            result.put("code", 400);
            result.put("message", "邮箱格式无效");
            return result;
        }
        try {
            String subject = "注册成功确认";
            String body = "您的账号已成功注册，邮箱 " + normalized + " 已验证。如非本人操作请尽快联系管理员。";
            emailSendService.send(null, normalized, subject, body);
            result.put("code", 0);
            result.put("message", "ok");
            return result;
        } catch (Exception e) {
            result.put("code", 502);
            result.put("message", "注册确认邮件发送失败: " + e.getMessage());
            return result;
        }
    }

    private Map<String, Object> sendCode(String channel, String target, String scene, boolean sendEmail) {
        Map<String, Object> result = new HashMap<>();
        if (target == null || target.isBlank()) {
            result.put("code", 400);
            result.put("message", channel + " 不能为空");
            return result;
        }
        if (scene == null || scene.isBlank() || !VALID_SCENES.contains(scene)) {
            result.put("code", 400);
            result.put("message", "无效的场景");
            return result;
        }

        String rateKey = "verify:rate:" + target;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(rateKey, "1", RATE_LIMIT_TTL);
        if (Boolean.FALSE.equals(acquired)) {
            result.put("code", 429);
            result.put("message", "发送过于频繁，请稍后再试");
            return result;
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        String codeKey = codeKey(channel, scene, target);
        redisTemplate.opsForValue().set(codeKey, code, CODE_TTL);

        if (sendEmail) {
            try {
                String subject = sceneLabel(scene) + "验证码";
                String body = "您的验证码是 " + code + "，5 分钟内有效。如非本人操作请忽略。";
                emailSendService.send(null, CredentialValidator.normalizeEmail(target), subject, body);
            } catch (Exception e) {
                redisTemplate.delete(codeKey);
                redisTemplate.delete(rateKey);
                result.put("code", 502);
                result.put("message", "邮件发送失败: " + e.getMessage());
                return result;
            }
        } else {
            System.out.println("[verify-code] channel=" + channel + ", target=" + target + ", scene=" + scene + ", code=" + code);
        }

        result.put("code", 0);
        result.put("message", "验证码已发送");
        return result;
    }

    private String sceneLabel(String scene) {
        return switch (scene) {
            case "REGISTER" -> "注册";
            case "LOGIN" -> "登录";
            case "PASSWORD_RESET" -> "重置密码";
            default -> "";
        };
    }

    private String codeKey(String channel, String scene, String target) {
        if ("email".equals(channel)) {
            target = CredentialValidator.normalizeEmail(target);
        }
        return "verify:" + channel + ":" + scene + ":" + target;
    }

    private String channelFromCredentialType(String credentialType, String target) {
        if ("PHONE".equalsIgnoreCase(credentialType)) {
            return "phone";
        }
        if ("EMAIL".equalsIgnoreCase(credentialType)) {
            return "email";
        }
        return target.contains("@") ? "email" : "phone";
    }
}
