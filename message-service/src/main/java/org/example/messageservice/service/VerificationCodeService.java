package org.example.messageservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class VerificationCodeService {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private EmailSendService emailSendService;
    @Autowired
    private CarrierService carrierService;
    @Autowired
    private MessageDispatchService dispatchService;

    private final Random random = new Random();

    public Map<String, Object> sendEmailCode(String email, String scene) {
        if (email == null || email.isEmpty()) return error(400, "邮箱不能为空");
        if (redisTemplate.hasKey("verify:rate:email:" + email)) return error(429, "发送过于频繁");

        String code = String.format("%06d", random.nextInt(1000000));
        redisTemplate.opsForValue().set("verify:email:" + scene + ":" + email, code, 5, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set("verify:rate:email:" + email, "1", 60, TimeUnit.SECONDS);

        return dispatchService.sendInstant(Map.of("channelType", "EMAIL", "receiver", email,
                "content", "您的验证码是：" + code + "（5分钟内有效）"));
    }

    public Map<String, Object> sendPhoneCode(String phone, String scene) {
        if (phone == null || phone.isEmpty()) return error(400, "手机号不能为空");
        if (redisTemplate.hasKey("verify:rate:phone:" + phone)) return error(429, "发送过于频繁");

        String code = String.format("%06d", random.nextInt(1000000));
        redisTemplate.opsForValue().set("verify:phone:" + scene + ":" + phone, code, 5, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set("verify:rate:phone:" + phone, "1", 60, TimeUnit.SECONDS);

        return dispatchService.sendInstant(Map.of("channelType", "SMS", "receiver", phone,
                "content", "您的验证码是：" + code + "（5分钟内有效）"));
    }

    public Map<String, Object> verify(Map<String, Object> request) {
        String credentialType = (String) request.get("credentialType");
        String target = (String) request.get("target");
        String scene = (String) request.get("scene");
        String code = (String) request.get("code");

        String prefix = "PHONE".equals(credentialType) ? "verify:phone:" : "verify:email:";
        String stored = redisTemplate.opsForValue().get(prefix + scene + ":" + target);
        if (stored == null) return error(400, "验证码不存在或已过期");
        if (!stored.equals(code)) return error(400, "验证码错误");
        redisTemplate.delete(prefix + scene + ":" + target);
        return ok(Map.of("valid", true));
    }

    private Map<String, Object> ok(Object data) { return Map.of("code", 0, "message", "ok", "data", data); }
    private Map<String, Object> error(int c, String m) { return Map.of("code", c, "message", m); }
}