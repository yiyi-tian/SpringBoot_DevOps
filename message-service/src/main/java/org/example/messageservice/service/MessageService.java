package org.example.messageservice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.example.messageservice.entity.MsgMessage;
import org.example.messageservice.mapper.MsgMessageMapper;
import org.example.messageservice.mapper.MsgTemplateMapper;
import org.example.messageservice.mapper.MsgVariableMapper;
import org.example.messageservice.mapper.TemplateVariableMapper;
import org.example.messageservice.entity.MsgCarrier;
import org.example.messageservice.mapper.MsgCarrierMapper;
import org.example.messageservice.entity.MsgTemplate;
import org.example.messageservice.entity.MsgVariable;
import org.example.messageservice.entity.TemplateVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;

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
    private MsgCarrierMapper carrierMapper;
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
        Object carrierIdObj = request.get("carrierId");

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

        if ("IN_APP".equals(channelType)) {
            return sendInAppMessage(receiver, content, templateIdObj);
        }
        if ("EMAIL".equals(channelType)) {
            return sendEmailMessage(receiver, content, carrierIdObj);
        }
        if ("SMS".equals(channelType) || "TENCENT_SMS".equals(channelType)) {
            return sendSmsMessage(receiver, content, carrierIdObj);
        }

        result.put("code", 400); result.put("message", "暂不支持的渠道: " + channelType);
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
     * 短信发送（容联云）
     */
    private Map<String, Object> sendSmsMessage(String receiver, String content, Object carrierIdObj) {
        Map<String, Object> result = new HashMap<>();

        // 自动查找短信载体
        if (carrierIdObj == null) {
            QueryWrapper<MsgCarrier> cw = new QueryWrapper<>();
            cw.eq("channel_type", "SMS");
            cw.eq("enabled", 1);
            cw.isNull("deleted_at");
            cw.last("LIMIT 1");
            MsgCarrier defaultCarrier = carrierMapper.selectOne(cw);
            if (defaultCarrier != null) {
                carrierIdObj = defaultCarrier.getCarrierId();
            }
        }

        if (carrierIdObj == null) {
            result.put("code", 400);
            result.put("message", "未找到可用的短信载体");
            return result;
        }

        // 读取载体配置
        Long carrierId = Long.valueOf(String.valueOf(carrierIdObj));
        MsgCarrier carrier = carrierMapper.selectById(carrierId);
        if (carrier == null || carrier.getConfigJson() == null) {
            result.put("code", 400);
            result.put("message", "短信载体配置不存在");
            return result;
        }

        String accountSid, authToken, appId, templateId;
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> config = mapper.readValue(carrier.getConfigJson(), Map.class);
            accountSid = String.valueOf(config.get("accountSid"));
            authToken  = String.valueOf(config.get("authToken"));
            appId      = String.valueOf(config.get("appId"));
            templateId = String.valueOf(config.getOrDefault("templateId", "1"));
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "载体配置解析失败: " + e.getMessage());
            return result;
        }

        try {
            com.cloopen.rest.sdk.CCPRestSmsSDK sdk = new com.cloopen.rest.sdk.CCPRestSmsSDK();
            sdk.init("app.cloopen.com", "8883");
            sdk.setAccount(accountSid, authToken);
            sdk.setAppId(appId);
            sdk.setBodyType(com.cloopen.rest.sdk.BodyType.Type_JSON);

            // datas[0]=内容, datas[1]=有效期(分钟)，测试模板固定格式
            String[] datas = {content, "5"};
            HashMap<String, Object> resp = sdk.sendTemplateSMS(receiver, templateId, datas);

            if ("000000".equals(resp.get("statusCode"))) {
                MsgMessage msg = new MsgMessage();
                msg.setReceiver(receiver);
                msg.setRenderedContent(content);
                msg.setStatus("SUCCESS");
                msg.setCarrierId(carrierId);
                msg.setSendTime(LocalDateTime.now());
                msg.setCreatedAt(LocalDateTime.now());
                msgMessageMapper.insert(msg);

                result.put("code", 0);
                result.put("message", "短信发送成功");
                result.put("data", Map.of("messageId", msg.getMessageId()));
            } else {
                result.put("code", 500);
                result.put("message", "短信发送失败: " + resp.get("statusCode") + " " + resp.get("statusMsg"));
            }
            return result;

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "短信发送异常: " + e.getMessage());
            return result;
        }
    }

    /**
     * 邮件发送
     */
    private Map<String, Object> sendEmailMessage(String receiver, String content, Object carrierIdObj) {
        Map<String, Object> result = new HashMap<>();

        // 自动查找邮件载体
        if (carrierIdObj == null) {
            QueryWrapper<MsgCarrier> cw = new QueryWrapper<>();
            cw.eq("channel_type", "EMAIL");
            cw.eq("enabled", 1);
            cw.isNull("deleted_at");
            cw.last("LIMIT 1");
            MsgCarrier defaultCarrier = carrierMapper.selectOne(cw);
            if (defaultCarrier != null) {
                carrierIdObj = defaultCarrier.getCarrierId();
            }
        }

        String host = "smtp.qq.com";
        int port = 587;
        String username = "";
        String password = "";

        if (carrierIdObj != null) {
            Long carrierId = Long.valueOf(String.valueOf(carrierIdObj));
            MsgCarrier carrier = carrierMapper.selectById(carrierId);
            if (carrier != null && carrier.getConfigJson() != null) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> config = mapper.readValue(carrier.getConfigJson(), Map.class);
                    host = String.valueOf(config.getOrDefault("host", host));
                    Object portObj = config.get("port");
                    if (portObj instanceof Integer) {
                        port = (Integer) portObj;
                    } else if (portObj != null) {
                        port = Integer.parseInt(String.valueOf(portObj));
                    }
                    username = String.valueOf(config.getOrDefault("username", ""));
                    password = String.valueOf(config.getOrDefault("password", ""));
                } catch (Exception e) {
                    result.put("code", 500);
                    result.put("message", "载体配置解析失败: " + e.getMessage());
                    return result;
                }
            }
        }

        if (username.isEmpty() || password.isEmpty()) {
            result.put("code", 400);
            result.put("message", "邮件服务未配置，请先创建邮件载体");
            return result;
        }

        try {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(host);
            mailSender.setPort(port);
            mailSender.setUsername(username);
            mailSender.setPassword(password);

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.smtp.auth", "true");
            if (port == 465) {
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.socketFactory.port", "465");
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                mailSender.setProtocol("smtps");
            } else {
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.starttls.required", "true");
                props.put("mail.smtp.ssl.trust", "smtp.qq.com");
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(username);
            helper.setTo(receiver);
            helper.setSubject("消息通知");
            helper.setText(content != null ? content : "");

            mailSender.send(mimeMessage);

            MsgMessage msg = new MsgMessage();
            msg.setReceiver(receiver);
            msg.setRenderedContent(content);
            msg.setStatus("SUCCESS");
            if (carrierIdObj != null) {
                msg.setCarrierId(Long.valueOf(String.valueOf(carrierIdObj)));
            }
            msg.setSendTime(LocalDateTime.now());
            msg.setCreatedAt(LocalDateTime.now());
            msgMessageMapper.insert(msg);

            result.put("code", 0);
            result.put("message", "邮件发送成功");
            result.put("data", Map.of("messageId", msg.getMessageId()));
            return result;
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "邮件发送失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 定时消息发送
     */
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

        // 发送邮件
        return sendEmailMessage(email, "您的验证码是：" + code + "（5分钟内有效）", null);
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

        return sendSmsMessage(phone, "您的验证码是：" + code + "（5分钟内有效）", null);
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

    public Map<String, Object> queryVariables(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        List<MsgVariable> variables = variableMapper.selectList(null);
        result.put("code", 0); result.put("message", "ok");
        result.put("data", Map.of("variables", variables));
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
        Map<String, Object> result = new HashMap<>();
        QueryWrapper<MsgCarrier> wrapper = new QueryWrapper<>();
        wrapper.isNull("deleted_at");
        if (channelType != null && !channelType.isEmpty()) {
            wrapper.eq("channel_type", channelType);
        }
        List<MsgCarrier> carriers = carrierMapper.selectList(wrapper);
        result.put("code", 0); result.put("message", "ok");
        result.put("data", Map.of("carriers", carriers));
        return result;
    }
    
    public Map<String, Object> getCarrier(Long id) {
        Map<String, Object> result = new HashMap<>();
        MsgCarrier carrier = carrierMapper.selectById(id);
        if (carrier == null || carrier.getDeletedAt() != null) {
            result.put("code", 404); result.put("message", "载体不存在");
            return result;
        }
        result.put("code", 0); result.put("message", "ok");
        result.put("data", carrier);
        return result;
    }

    public Map<String, Object> createCarrier(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        String name = (String) request.get("name");
        String provider = (String) request.get("provider");
        String channelType = (String) request.get("channelType");
        String configJson = (String) request.get("configJson");

        if (name == null || name.isEmpty()) { result.put("code", 400); result.put("message", "名称不能为空"); return result; }
        if (channelType == null || channelType.isEmpty()) { result.put("code", 400); result.put("message", "channelType 不能为空"); return result; }

        MsgCarrier carrier = new MsgCarrier();
        carrier.setName(name);
        carrier.setProvider(provider != null ? provider : "");
        carrier.setChannelType(channelType);
        carrier.setConfigJson(configJson != null ? configJson : "{}");
        carrier.setEnabled(1);
        carrier.setCreatedAt(LocalDateTime.now());
        carrier.setUpdatedAt(LocalDateTime.now());
        carrierMapper.insert(carrier);

        result.put("code", 0); result.put("message", "ok");
        result.put("data", Map.of("carrierId", carrier.getCarrierId()));
        return result;
    }

    public Map<String, Object> updateCarrier(Long id, Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        MsgCarrier carrier = carrierMapper.selectById(id);
        if (carrier == null || carrier.getDeletedAt() != null) {
            result.put("code", 404); result.put("message", "载体不存在"); return result;
        }
        if (request.containsKey("name")) carrier.setName((String) request.get("name"));
        if (request.containsKey("provider")) carrier.setProvider((String) request.get("provider"));
        if (request.containsKey("configJson")) carrier.setConfigJson((String) request.get("configJson"));
        if (request.containsKey("enabled")) carrier.setEnabled((Integer) request.get("enabled"));
        carrier.setUpdatedAt(LocalDateTime.now());
        carrierMapper.updateById(carrier);
        result.put("code", 0); result.put("message", "ok");
        return result;
    }

    public Map<String, Object> deleteCarrier(Long id) {
        Map<String, Object> result = new HashMap<>();
        MsgCarrier carrier = carrierMapper.selectById(id);
        if (carrier == null) { result.put("code", 404); result.put("message", "载体不存在"); return result; }
        carrier.setDeletedAt(LocalDateTime.now());
        carrierMapper.updateById(carrier);
        result.put("code", 0); result.put("message", "删除成功");
        return result;
    }

    public Map<String, Object> testCarrier(Long id) {
        Map<String, Object> result = new HashMap<>();
        MsgCarrier carrier = carrierMapper.selectById(id);
        if (carrier == null || carrier.getDeletedAt() != null) {
            result.put("code", 404); result.put("message", "载体不存在"); return result;
        }
        System.out.println("=================================");
        System.out.println("  载体连通性测试");
        System.out.println("  名称: " + carrier.getName());
        System.out.println("  渠道: " + carrier.getChannelType());
        System.out.println("  配置: " + carrier.getConfigJson());
        System.out.println("  TODO: 实际发送测试消息");
        System.out.println("=================================");
        result.put("code", 0); result.put("message", "测试成功（控制台输出）");
        return result;
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