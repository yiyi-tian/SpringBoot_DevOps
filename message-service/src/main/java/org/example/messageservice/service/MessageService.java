package org.example.messageservice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.messageservice.entity.MsgMessage;
import org.example.messageservice.mapper.MsgMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 消息服务：发送、验证码、模板、变量、载体
 */
@Service
public class MessageService {

    @Autowired
    private MsgMessageMapper msgMessageMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // ==================== 消息发送 ====================

    /**
     * 即时消息发送
     */
    public Map<String, Object> sendInstant(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        String channelType = (String) request.get("channelType");
        String receiver = (String) request.get("receiver");
        String content = (String) request.get("content");
        Object templateIdObj = request.get("templateId");

        // 参数校验
        if (channelType == null || channelType.isEmpty()) {
            result.put("code", 400);
            result.put("message", "channelType 不能为空");
            return result;
        }
        if (receiver == null || receiver.isEmpty()) {
            result.put("code", 400);
            result.put("message", "receiver 不能为空");
            return result;
        }

        // 处理站内信
        if ("IN_APP".equals(channelType)) {
            return sendInAppMessage(receiver, content, templateIdObj);
        }

        // 其他渠道 TODO
        result.put("code", 400);
        result.put("message", "暂不支持的渠道类型: " + channelType);
        return result;
    }

    /**
     * 站内信发送
     */
    private Map<String, Object> sendInAppMessage(String receiver, String content, Object templateIdObj) {
        Map<String, Object> result = new HashMap<>();

        MsgMessage message = new MsgMessage();
        message.setReceiver(receiver);
        message.setRenderedContent(content != null ? content : "");
        message.setStatus("SUCCESS");
        message.setSendTime(LocalDateTime.now());
        message.setCreatedAt(LocalDateTime.now());

        if (templateIdObj != null) {
            message.setTemplateId(Long.valueOf(String.valueOf(templateIdObj)));
        }

        msgMessageMapper.insert(message);

        Map<String, Object> data = new HashMap<>();
        data.put("messageId", message.getMessageId());
        result.put("code", 0);
        result.put("message", "ok");
        result.put("data", data);
        return result;
    }

    /**
     * 短信发送
     */
    private void sendSmsMessage(Map<String, Object> request) {
        // TODO: 根据 carrierId 查询 msg_carrier 获取腾讯云配置（secretId, secretKey, sdkAppId 等）
        // TODO: 调用腾讯云 SMS SDK 发送短信
        // TODO: 记录 provider_msg_id（第三方回执）
    }

    /**
     * 邮件发送
     */
    private void sendEmailMessage(Map<String, Object> request) {
        // TODO: 根据 carrierId 查询 msg_carrier 获取 SMTP 配置（host, port, username, password）
        // TODO: 调用 Spring Mail (JavaMailSender) 发送邮件
    }

    /**
     * 定时消息发送
     */
    public Map<String, Object> sendScheduled(Map<String, Object> request) {
        // TODO: 解析参数：channelType, templateId, receiver, variables, scheduledAt
        // TODO: 写入 msg_task 表（is_scheduled=1, status=PENDING）
        // TODO: 返回 {"code":0, "data":{"taskId":xxx}}
        throw new UnsupportedOperationException("TODO: 实现定时消息发送");
    }

    // ==================== 验证码 ====================

    /**
     * 发送邮箱验证码
     */
    public Map<String, Object> sendEmailCode(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        String email = (String) request.get("email");
        String scene = (String) request.getOrDefault("scene", "REGISTER");

        if (email == null || email.isEmpty()) {
            result.put("code", 400); result.put("message", "邮箱不能为空"); return result;
        }

        // 限流检查
        String rateKey = "verify:rate:email:" + email;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(rateKey))) {
            result.put("code", 429); result.put("message", "发送过于频繁，请60秒后再试"); return result;
        }

        // 生成6位验证码
        String code = String.format("%06d", new Random().nextInt(1000000));
        String codeKey = "verify:email:" + scene + ":" + email;

        // 存入 Redis（5分钟有效）
        redisTemplate.opsForValue().set(codeKey, code, 5, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(rateKey, "1", 60, TimeUnit.SECONDS);

        // TODO: 调用邮件服务发送验证码（当前仅打印到控制台）
        System.out.println("=================================");
        System.out.println("  验证码: " + code);
        System.out.println("  发送到: " + email);
        System.out.println("  场景:   " + scene);
        System.out.println("=================================");

        result.put("code", 0); result.put("message", "验证码已发送"); return result;
    }

    /**
     * 发送手机验证码
     */
    public Map<String, Object> sendPhoneCode(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        String phone = (String) request.get("phone");
        String scene = (String) request.getOrDefault("scene", "REGISTER");

        if (phone == null || phone.isEmpty()) {
            result.put("code", 400); result.put("message", "手机号不能为空"); return result;
        }

        String rateKey = "verify:rate:phone:" + phone;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(rateKey))) {
            result.put("code", 429); result.put("message", "发送过于频繁，请60秒后再试"); return result;
        }

        String code = String.format("%06d", new Random().nextInt(1000000));
        String codeKey = "verify:phone:" + scene + ":" + phone;

        redisTemplate.opsForValue().set(codeKey, code, 5, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(rateKey, "1", 60, TimeUnit.SECONDS);

        System.out.println("=================================");
        System.out.println("  验证码: " + code);
        System.out.println("  发送到: " + phone);
        System.out.println("  场景:   " + scene);
        System.out.println("=================================");

        result.put("code", 0); result.put("message", "验证码已发送"); return result;
    }

    /**
     * 校验验证码
     */
    public Map<String, Object> verifyCode(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        String credentialType = (String) request.get("credentialType");
        String target = (String) request.get("target");
        String scene = (String) request.get("scene");
        String code = (String) request.get("code");

        String prefix = "PHONE".equals(credentialType) ? "verify:phone:" : "verify:email:";
        String codeKey = prefix + scene + ":" + target;

        String storedCode = redisTemplate.opsForValue().get(codeKey);
        if (storedCode == null) {
            result.put("code", 400); result.put("message", "验证码不存在或已过期"); return result;
        }
        if (!storedCode.equals(code)) {
            result.put("code", 400); result.put("message", "验证码错误"); return result;
        }

        // 一次性有效：校验成功后删除
        redisTemplate.delete(codeKey);

        result.put("code", 0); result.put("message", "验证码校验成功");
        Map<String, Object> data = new HashMap<>();
        data.put("valid", true);
        result.put("data", data);
        return result;
    }

    // ==================== 消息模板 ====================

    public Map<String, Object> createTemplate(Map<String, Object> request) {
        // TODO: 提取参数：name, content, channelType
        // TODO: 渠道特有校验：
        //       短信：签名合规性、计费分条、敏感词过滤
        //       邮件：标题格式（非空、非特殊字符）、附件规格
        //       飞书/微信：JSON 结构验证、ID 校验
        //       站内信：渲染兼容性、消息长度
        // TODO: 创建 msg_template 记录（status=DRAFT）
        // TODO: 返回 {"code":0, "data":{"templateId":xxx}}
        throw new UnsupportedOperationException("TODO: 实现模板创建");
    }

    public Map<String, Object> queryTemplates(Map<String, Object> params) {
        // TODO: 支持分页：page, size
        // TODO: 支持筛选：channelType, status, keyword
        // TODO: 返回 {"code":0, "data":{"list":[...], "total":100}}
        throw new UnsupportedOperationException("TODO: 实现模板查询");
    }

    public Map<String, Object> updateTemplate(Map<String, Object> request) {
        // TODO: 提取 templateId 和要更新的字段（name, content, status 等）
        // TODO: 更新 msg_template 表
        // TODO: 如果状态变更，同步刷新缓存（Redis）
        // TODO: 返回 {"code":0, "message":"ok"}
        throw new UnsupportedOperationException("TODO: 实现模板更新");
    }

    // ==================== 模板变量 ====================

    public Map<String, Object> getVariableSchema() {
        // TODO: 返回变量定义规则（哪些字段是必填、格式要求等）
        throw new UnsupportedOperationException("TODO: 实现变量规则查询");
    }

    public Map<String, Object> createVariable(Map<String, Object> request) {
        // TODO: 创建 msg_variable 记录
        // TODO: 关联 template_variable（绑定变量和模板）
        throw new UnsupportedOperationException("TODO: 实现变量创建");
    }

    public Map<String, Object> getVariable(String variableId) {
        // TODO: 查询 msg_variable 表
        throw new UnsupportedOperationException("TODO: 实现变量查询");
    }

    public Map<String, Object> updateVariable(String variableId, Map<String, Object> request) {
        // TODO: 更新 msg_variable 表
        throw new UnsupportedOperationException("TODO: 实现变量更新");
    }

    public Map<String, Object> deleteVariable(String variableId) {
        // TODO: 删除 msg_variable 和关联的 template_variable 记录
        throw new UnsupportedOperationException("TODO: 实现变量删除");
    }

    // ==================== 载体管理 ====================

    public Map<String, Object> getCarriers(String channelType) {
        // TODO: 查询 msg_carrier 表，可选按 channelType 筛选
        // TODO: 敏感信息（config_json）需要脱敏后返回
        throw new UnsupportedOperationException("TODO: 实现载体查询");
    }

    public Map<String, Object> getCarrier(Long id) {
        // TODO: 查询 msg_carrier 详情
        // TODO: 敏感信息脱敏
        throw new UnsupportedOperationException("TODO: 实现载体详情");
    }

    public Map<String, Object> createCarrier(Map<String, Object> request) {
        // TODO: 提取参数：name, provider, channelType, configJson
        // TODO: 加密存储 configJson 中的敏感字段（secretKey, password 等）
        // TODO: 创建 msg_carrier 记录
        throw new UnsupportedOperationException("TODO: 实现载体创建");
    }

    public Map<String, Object> updateCarrier(Long id, Map<String, Object> request) {
        // TODO: 更新 msg_carrier 表
        // TODO: 清除 Redis 缓存：carrier:config:{id}
        throw new UnsupportedOperationException("TODO: 实现载体更新");
    }

    public Map<String, Object> deleteCarrier(Long id) {
        // TODO: 软删除 msg_carrier（设置 deleted_at）
        throw new UnsupportedOperationException("TODO: 实现载体删除");
    }

    public Map<String, Object> testCarrier(Long id) {
        // TODO: 查询载体配置
        // TODO: 根据 channelType 执行连通性测试：
        //       TENCENT_SMS → 调用腾讯云 API 发一条测试短信
        //       EMAIL → 发一封测试邮件
        // TODO: 返回 {"code":0, "message":"测试成功"}
        throw new UnsupportedOperationException("TODO: 实现载体测试");
    }

    // ==================== 发送记录 ====================

    public Map<String, Object> getSendingRecords(Map<String, Object> params) {
        // TODO: 查询 msg_message 表
        // TODO: 支持分页和筛选（channelType, status, start_time, end_time）
        throw new UnsupportedOperationException("TODO: 实现发送记录查询");
    }

    public Map<String, Object> deleteSendingRecord(Map<String, Object> request) {
        // TODO: 提取 messageId，删除 msg_message 记录
        throw new UnsupportedOperationException("TODO: 实现发送记录删除");
    }

    // ==================== 调度触发 ====================

    public Map<String, Object> triggerScheduler() {
        // TODO: 扫描 msg_task 表（is_scheduled=1, status=PENDING, scheduled_at <= now）
        // TODO: 逐条执行发送
        // TODO: 更新 task 状态：SUCCESS / FAILED
        // TODO: 写入 msg_message 发送记录
        throw new UnsupportedOperationException("TODO: 实现调度触发");
    }

    // ==================== 信箱查询 ====================

    public Map<String, Object> getInbox(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();

        String receiver = (String) params.get("receiver");
        Integer page = params.get("page") != null ? Integer.valueOf(String.valueOf(params.get("page"))) : 1;
        Integer size = params.get("size") != null ? Integer.valueOf(String.valueOf(params.get("size"))) : 20;

        if (receiver == null || receiver.isEmpty()) {
            result.put("code", 400);
            result.put("message", "receiver 不能为空");
            return result;
        }

        QueryWrapper<MsgMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("receiver", receiver)
               .orderByDesc("created_at");

        // 分页
        long total = msgMessageMapper.selectCount(wrapper);
        wrapper.last("LIMIT " + ((page - 1) * size) + ", " + size);
        List<MsgMessage> messages = msgMessageMapper.selectList(wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("messages", messages);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);

        result.put("code", 0);
        result.put("message", "ok");
        result.put("data", data);
        return result;
    }
}