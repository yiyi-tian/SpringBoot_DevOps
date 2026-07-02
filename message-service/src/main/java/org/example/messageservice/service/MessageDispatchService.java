package org.example.messageservice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.messageservice.entity.MsgCarrier;
import org.example.messageservice.entity.MsgMessage;
import org.example.messageservice.mapper.MsgMessageMapper;
import org.example.messageservice.mapper.MsgCarrierMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class MessageDispatchService {

    @Autowired
    private MsgMessageMapper msgMessageMapper;
    @Autowired
    private MsgCarrierMapper carrierMapper;
    @Autowired
    private CarrierService carrierService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> sendInstant(Map<String, Object> request) {
        String channelType = (String) request.get("channelType");
        String receiver = (String) request.get("receiver");
        String content = (String) request.get("content");
        Object templateIdObj = request.get("templateId");
        Object carrierIdObj = request.get("carrierId");

        if (channelType == null || channelType.isEmpty()) return error(400, "channelType 不能为空");
        if (receiver == null || receiver.isEmpty()) return error(400, "receiver 不能为空");

        if ("IN_APP".equals(channelType)) return sendInApp(receiver, content, templateIdObj);
        if ("EMAIL".equals(channelType)) return sendEmail(receiver, content, carrierIdObj);
        if ("SMS".equals(channelType) || "TENCENT_SMS".equals(channelType)) return sendSms(receiver, content, carrierIdObj);

        return error(400, "暂不支持的渠道: " + channelType);
    }

    public Map<String, Object> sendScheduled(Map<String, Object> request) {
        String receiver = (String) request.get("receiver");
        String content = (String) request.get("content");
        MsgMessage msg = new MsgMessage();
        msg.setReceiver(receiver);
        msg.setRenderedContent(content);
        msg.setStatus("PENDING");
        msg.setCreatedAt(LocalDateTime.now());
        msgMessageMapper.insert(msg);
        return ok(Map.of("messageId", msg.getMessageId()));
    }

    public Map<String, Object> getSendingRecords(Map<String, Object> params) {
        int page = intVal(params.get("page"), 1);
        int size = intVal(params.get("size"), 20);
        QueryWrapper<MsgMessage> w = new QueryWrapper<>();
        w.orderByDesc("created_at");
        long total = msgMessageMapper.selectCount(w);
        w.last("LIMIT " + ((page - 1) * size) + ", " + size);
        return ok(Map.of("records", msgMessageMapper.selectList(w), "total", total));
    }

    public Map<String, Object> deleteSendingRecord(Map<String, Object> request) {
        Object idObj = request.get("id");
        if (idObj == null) return error(400, "id 不能为空");
        msgMessageMapper.deleteById(Long.valueOf(String.valueOf(idObj)));
        return ok();
    }

    public Map<String, Object> getInbox(Map<String, Object> params) {
        String receiver = (String) params.get("receiver");
        if (receiver == null || receiver.isEmpty()) return error(400, "receiver 不能为空");
        int page = intVal(params.get("page"), 1);
        int size = intVal(params.get("size"), 20);
        QueryWrapper<MsgMessage> w = new QueryWrapper<>();
        w.eq("receiver", receiver).orderByDesc("created_at");
        long total = msgMessageMapper.selectCount(w);
        w.last("LIMIT " + ((page - 1) * size) + ", " + size);
        return ok(Map.of("messages", msgMessageMapper.selectList(w), "total", total, "page", page, "size", size));
    }

    public Map<String, Object> triggerScheduler() {
        QueryWrapper<MsgMessage> w = new QueryWrapper<>();
        w.eq("status", "PENDING");
        int count = 0;
        for (MsgMessage msg : msgMessageMapper.selectList(w)) {
            msg.setStatus("SUCCESS");
            msg.setSendTime(LocalDateTime.now());
            msgMessageMapper.updateById(msg);
            count++;
        }
        return ok(Map.of("processed", count));
    }

    // --- private ---

    private Map<String, Object> sendInApp(String receiver, String content, Object templateIdObj) {
        MsgMessage msg = new MsgMessage();
        msg.setReceiver(receiver);
        msg.setRenderedContent(content != null ? content : "");
        msg.setStatus("SUCCESS");
        msg.setSendTime(LocalDateTime.now());
        msg.setCreatedAt(LocalDateTime.now());
        if (templateIdObj != null) msg.setTemplateId(Long.valueOf(String.valueOf(templateIdObj)));
        msgMessageMapper.insert(msg);
        return ok(Map.of("messageId", msg.getMessageId()));
    }

    private Map<String, Object> sendEmail(String receiver, String content, Object carrierIdObj) {
        MsgCarrier carrier = carrierService.resolveCarrier("EMAIL", carrierIdObj != null ? Long.valueOf(String.valueOf(carrierIdObj)) : null);
        if (carrier == null) return error(400, "邮件服务未配置，请先创建邮件载体");
        try {
            Map<String, Object> config = objectMapper.readValue(carrier.getConfigJson(), Map.class);
            String host = String.valueOf(config.getOrDefault("host", "smtp.qq.com"));
            int port = intVal(config.get("port"), 587);
            String username = String.valueOf(config.getOrDefault("username", ""));
            String password = String.valueOf(config.getOrDefault("password", ""));

            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(host);
            sender.setPort(port);
            sender.setUsername(username);
            sender.setPassword(password);

            Properties props = sender.getJavaMailProperties();
            props.put("mail.smtp.auth", "true");
            if (port == 465) {
                props.put("mail.smtp.ssl.enable", "true");
                sender.setProtocol("smtps");
            } else {
                props.put("mail.smtp.starttls.enable", "true");
            }

            MimeMessage mime = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(username);
            helper.setTo(receiver);
            helper.setSubject("消息通知");
            helper.setText(content != null ? content : "");
            sender.send(mime);

            MsgMessage msg = new MsgMessage();
            msg.setReceiver(receiver);
            msg.setRenderedContent(content);
            msg.setStatus("SUCCESS");
            msg.setCarrierId(carrier.getCarrierId());
            msg.setSendTime(LocalDateTime.now());
            msg.setCreatedAt(LocalDateTime.now());
            msgMessageMapper.insert(msg);
            return ok(Map.of("messageId", msg.getMessageId()));
        } catch (Exception e) {
            return error(500, "邮件发送失败: " + e.getMessage());
        }
    }

    private Map<String, Object> sendSms(String receiver, String content, Object carrierIdObj) {
        MsgCarrier carrier = carrierService.resolveCarrier("SMS", carrierIdObj != null ? Long.valueOf(String.valueOf(carrierIdObj)) : null);
        if (carrier == null) return error(400, "未找到可用的短信载体");
        try {
            Map<String, Object> config = objectMapper.readValue(carrier.getConfigJson(), Map.class);
            String accountSid = String.valueOf(config.get("accountSid"));
            String authToken = String.valueOf(config.get("authToken"));
            String appId = String.valueOf(config.get("appId"));
            String templateId = String.valueOf(config.getOrDefault("templateId", "1"));

            com.cloopen.rest.sdk.CCPRestSmsSDK sdk = new com.cloopen.rest.sdk.CCPRestSmsSDK();
            sdk.init("app.cloopen.com", "8883");
            sdk.setAccount(accountSid, authToken);
            sdk.setAppId(appId);
            sdk.setBodyType(com.cloopen.rest.sdk.BodyType.Type_JSON);
            HashMap<String, Object> resp = sdk.sendTemplateSMS(receiver, templateId, new String[]{content, "5"});

            MsgMessage msg = new MsgMessage();
            msg.setReceiver(receiver);
            msg.setRenderedContent(content);
            msg.setCarrierId(carrier.getCarrierId());
            msg.setSendTime(LocalDateTime.now());
            msg.setCreatedAt(LocalDateTime.now());

            if ("000000".equals(resp.get("statusCode"))) {
                msg.setStatus("SUCCESS");
                msgMessageMapper.insert(msg);
                return ok(Map.of("messageId", msg.getMessageId()));
            } else {
                msg.setStatus("FAILED");
                msgMessageMapper.insert(msg);
                return error(500, "短信发送失败: " + resp.get("statusMsg"));
            }
        } catch (Exception e) {
            return error(500, "短信发送异常: " + e.getMessage());
        }
    }

    private int intVal(Object v, int d) { return v == null ? d : Integer.parseInt(String.valueOf(v)); }
    private Map<String, Object> ok() { return Map.of("code", 0, "message", "ok"); }
    private Map<String, Object> ok(Object data) { return Map.of("code", 0, "message", "ok", "data", data); }
    private Map<String, Object> error(int code, String msg) { return Map.of("code", code, "message", msg); }
}