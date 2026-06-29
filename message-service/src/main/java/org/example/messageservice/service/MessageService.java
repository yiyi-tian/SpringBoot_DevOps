package org.example.messageservice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.messageservice.entity.MsgMessage;
import org.example.messageservice.mapper.MsgMessageMapper;
import org.example.messageservice.mapper.MsgTemplateMapper;
import org.example.messageservice.mapper.MsgVariableMapper;
import org.example.messageservice.mapper.TemplateVariableMapper;
import org.example.messageservice.entity.MsgTemplate;
import org.example.messageservice.entity.MsgVariable;
import org.example.messageservice.entity.TemplateVariable;
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
    private MsgTemplateMapper templateMapper;
    @Autowired
    private MsgVariableMapper variableMapper;
    @Autowired
    private TemplateVariableMapper templateVariableMapper;

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
        Map<String, Object> result = new HashMap<>();
        String name = (String) request.get("name");
        String content = (String) request.get("content");
        String channelType = (String) request.get("channelType");

        if (name == null || name.isEmpty()) { result.put("code", 400); result.put("message", "模板名称不能为空"); return result; }
        if (content == null || content.isEmpty()) { result.put("code", 400); result.put("message", "模板内容不能为空"); return result; }
        if (channelType == null || channelType.isEmpty()) { result.put("code", 400); result.put("message", "channelType 不能为空"); return result; }

        MsgTemplate template = new MsgTemplate();
        template.setName(name);
        template.setContent(content);
        template.setChannelType(channelType);
        template.setStatus("DRAFT");
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        templateMapper.insert(template);

        // 如果有变量列表，绑定变量
        List<Map<String, Object>> variables = (List<Map<String, Object>>) request.get("variables");
        if (variables != null) {
            bindVariablesToTemplate(template.getTemplateId(), variables);
        }

        result.put("code", 0); result.put("message", "ok");
        result.put("data", Map.of("templateId", template.getTemplateId()));
        return result;
    }

    public Map<String, Object> queryTemplates(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        Integer page = params.get("page") != null ? Integer.valueOf(String.valueOf(params.get("page"))) : 1;
        Integer size = params.get("size") != null ? Integer.valueOf(String.valueOf(params.get("size"))) : 20;
        String channelType = (String) params.get("channelType");
        String status = (String) params.get("status");

        QueryWrapper<MsgTemplate> wrapper = new QueryWrapper<>();
        if (channelType != null && !channelType.isEmpty()) wrapper.eq("channel_type", channelType);
        if (status != null && !status.isEmpty()) wrapper.eq("status", status);
        wrapper.orderByDesc("created_at");

        long total = templateMapper.selectCount(wrapper);
        wrapper.last("LIMIT " + ((page - 1) * size) + ", " + size);
        List<MsgTemplate> templates = templateMapper.selectList(wrapper);

        result.put("code", 0); result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("templates", templates);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        result.put("data", data);
        return result;
    }

    public Map<String, Object> getTemplate(Long templateId) {
        Map<String, Object> result = new HashMap<>();
        MsgTemplate template = templateMapper.selectById(templateId);
        if (template == null) { result.put("code", 404); result.put("message", "模板不存在"); return result; }

        // 查询关联的变量
        QueryWrapper<TemplateVariable> tvWrapper = new QueryWrapper<>();
        tvWrapper.eq("template_id", templateId);
        List<TemplateVariable> bindings = templateVariableMapper.selectList(tvWrapper);
        List<Map<String, Object>> variables = new ArrayList<>();
        for (TemplateVariable tv : bindings) {
            MsgVariable variable = variableMapper.selectById(tv.getVariableId());
            if (variable != null) {
                Map<String, Object> varMap = new HashMap<>();
                varMap.put("variableId", variable.getVariableId());
                varMap.put("varKey", variable.getVarKey());
                varMap.put("name", variable.getName());
                varMap.put("type", variable.getType());
                varMap.put("required", tv.getRequiredOverride() != null ? tv.getRequiredOverride() : variable.getRequired());
                varMap.put("defaultValue", tv.getDefaultOverride() != null ? tv.getDefaultOverride() : variable.getDefaultValue());
                variables.add(varMap);
            }
        }

        result.put("code", 0); result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("template", template);
        data.put("variables", variables);
        result.put("data", data);
        return result;
    }

    public Map<String, Object> updateTemplate(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        Object idObj = request.get("id");
        if (idObj == null) { result.put("code", 400); result.put("message", "id 不能为空"); return result; }
        Long templateId = Long.valueOf(String.valueOf(idObj));

        MsgTemplate template = templateMapper.selectById(templateId);
        if (template == null) { result.put("code", 404); result.put("message", "模板不存在"); return result; }

        if (request.containsKey("name")) template.setName((String) request.get("name"));
        if (request.containsKey("content")) template.setContent((String) request.get("content"));
        if (request.containsKey("channelType")) template.setChannelType((String) request.get("channelType"));
        if (request.containsKey("status")) template.setStatus((String) request.get("status"));
        template.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(template);

        // 如果传了变量列表，先删旧绑定再重新绑定
        List<Map<String, Object>> variables = (List<Map<String, Object>>) request.get("variables");
        if (variables != null) {
            QueryWrapper<TemplateVariable> tvWrapper = new QueryWrapper<>();
            tvWrapper.eq("template_id", templateId);
            templateVariableMapper.delete(tvWrapper);
            bindVariablesToTemplate(templateId, variables);
        }

        result.put("code", 0); result.put("message", "ok");
        return result;
    }

    public Map<String, Object> deleteTemplate(Long templateId) {
        Map<String, Object> result = new HashMap<>();
        MsgTemplate template = templateMapper.selectById(templateId);
        if (template == null) { result.put("code", 404); result.put("message", "模板不存在"); return result; }

        // 删除关联的变量绑定
        QueryWrapper<TemplateVariable> tvWrapper = new QueryWrapper<>();
        tvWrapper.eq("template_id", templateId);
        templateVariableMapper.delete(tvWrapper);

        templateMapper.deleteById(templateId);
        result.put("code", 0); result.put("message", "删除成功");
        return result;
    }

    private void bindVariablesToTemplate(Long templateId, List<Map<String, Object>> variables) {
        for (Map<String, Object> varMap : variables) {
            Long variableId = Long.valueOf(String.valueOf(varMap.get("variableId")));
            TemplateVariable tv = new TemplateVariable();
            tv.setTemplateId(templateId);
            tv.setVariableId(variableId);
            if (varMap.containsKey("requiredOverride")) {
                tv.setRequiredOverride(Integer.valueOf(String.valueOf(varMap.get("requiredOverride"))));
            }
            if (varMap.containsKey("defaultOverride")) {
                tv.setDefaultOverride((String) varMap.get("defaultOverride"));
            }
            tv.setCreatedAt(LocalDateTime.now());
            templateVariableMapper.insert(tv);
        }
    }
    
    // ==================== 模板变量 ====================

    public Map<String, Object> getVariableSchema() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0); result.put("message", "ok");
        result.put("data", Map.of("types", List.of("STRING", "NUMBER", "DATE")));
        return result;
    }

    public Map<String, Object> createVariable(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        String varKey = (String) request.get("varKey");
        String name = (String) request.get("name");
        String type = (String) request.getOrDefault("type", "STRING");

        if (varKey == null || varKey.isEmpty()) { result.put("code", 400); result.put("message", "varKey 不能为空"); return result; }
        if (name == null || name.isEmpty()) { result.put("code", 400); result.put("message", "变量名不能为空"); return result; }

        MsgVariable variable = new MsgVariable();
        variable.setVarKey(varKey);
        variable.setName(name);
        variable.setType(type);
        variable.setRequired((Integer) request.getOrDefault("required", 1));
        variable.setDefaultValue((String) request.get("defaultValue"));
        variable.setScope((String) request.getOrDefault("scope", "GLOBAL"));
        variable.setStatus("ACTIVE");
        variable.setDescription((String) request.get("description"));
        variable.setCreatedAt(LocalDateTime.now());
        variable.setUpdatedAt(LocalDateTime.now());
        variableMapper.insert(variable);

        result.put("code", 0); result.put("message", "ok");
        result.put("data", Map.of("variableId", variable.getVariableId()));
        return result;
    }

    public Map<String, Object> getVariable(Long variableId) {
        Map<String, Object> result = new HashMap<>();
        MsgVariable variable = variableMapper.selectById(variableId);
        if (variable == null) { result.put("code", 404); result.put("message", "变量不存在"); return result; }
        result.put("code", 0); result.put("message", "ok");
        result.put("data", variable);
        return result;
    }

    public Map<String, Object> updateVariable(Long variableId, Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        MsgVariable variable = variableMapper.selectById(variableId);
        if (variable == null) { result.put("code", 404); result.put("message", "变量不存在"); return result; }
        if (request.containsKey("name")) variable.setName((String) request.get("name"));
        if (request.containsKey("type")) variable.setType((String) request.get("type"));
        if (request.containsKey("required")) variable.setRequired((Integer) request.get("required"));
        if (request.containsKey("defaultValue")) variable.setDefaultValue((String) request.get("defaultValue"));
        if (request.containsKey("description")) variable.setDescription((String) request.get("description"));
        variable.setUpdatedAt(LocalDateTime.now());
        variableMapper.updateById(variable);
        result.put("code", 0); result.put("message", "ok");
        return result;
    }

    public Map<String, Object> deleteVariable(Long variableId) {
        Map<String, Object> result = new HashMap<>();
        variableMapper.deleteById(variableId);
        result.put("code", 0); result.put("message", "ok");
        return result;
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
        Map<String, Object> result = new HashMap<>();
        Integer page = params.get("page") != null ? Integer.valueOf(String.valueOf(params.get("page"))) : 1;
        Integer size = params.get("size") != null ? Integer.valueOf(String.valueOf(params.get("size"))) : 20;

        QueryWrapper<MsgMessage> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("created_at");
        long total = msgMessageMapper.selectCount(wrapper);
        wrapper.last("LIMIT " + ((page - 1) * size) + ", " + size);
        List<MsgMessage> records = msgMessageMapper.selectList(wrapper);

        result.put("code", 0); result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", total);
        result.put("data", data);
        return result;
    }

    public Map<String, Object> deleteSendingRecord(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        Object idObj = request.get("id");
        if (idObj == null) { result.put("code", 400); result.put("message", "id 不能为空"); return result; }
        msgMessageMapper.deleteById(Long.valueOf(String.valueOf(idObj)));
        result.put("code", 0); result.put("message", "ok");
        return result;
    }

    // ==================== 调度触发 ====================

    public Map<String, Object> sendScheduled(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        // 简单实现：写入 msg_message，状态 PENDING
        String receiver = (String) request.get("receiver");
        String content = (String) request.get("content");
        MsgMessage message = new MsgMessage();
        message.setReceiver(receiver);
        message.setRenderedContent(content);
        message.setStatus("PENDING");
        message.setCreatedAt(LocalDateTime.now());
        msgMessageMapper.insert(message);
        result.put("code", 0); result.put("message", "定时任务已创建");
        result.put("data", Map.of("messageId", message.getMessageId()));
        return result;
    }

    public Map<String, Object> triggerScheduler() {
        Map<String, Object> result = new HashMap<>();
        // 扫描 PENDING 消息，改为 SUCCESS
        QueryWrapper<MsgMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "PENDING");
        List<MsgMessage> pendingList = msgMessageMapper.selectList(wrapper);
        int count = 0;
        for (MsgMessage msg : pendingList) {
            msg.setStatus("SUCCESS");
            msg.setSendTime(LocalDateTime.now());
            msgMessageMapper.updateById(msg);
            count++;
        }
        result.put("code", 0); result.put("message", "调度完成");
        result.put("data", Map.of("processed", count));
        return result;
    }
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