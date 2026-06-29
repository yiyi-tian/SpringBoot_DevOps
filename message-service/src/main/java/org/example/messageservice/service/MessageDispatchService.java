package org.example.messageservice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.messageservice.entity.MsgCarrier;
import org.example.messageservice.entity.MsgMessage;
import org.example.messageservice.entity.MsgTemplate;
import org.example.messageservice.mapper.MsgMessageMapper;
import org.example.messageservice.support.ServiceResults;
import org.example.messageservice.support.TemplateRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MessageDispatchService {

    @Autowired
    private TemplateService templateService;

    @Autowired
    private CarrierService carrierService;

    @Autowired
    private EmailSendService emailSendService;

    @Autowired
    private MsgMessageMapper messageMapper;

    @SuppressWarnings("unchecked")
    public Map<String, Object> sendInstant(Map<String, Object> request) {
        String channelType = stringVal(request.get("channelType"));
        String receiver = stringVal(request.get("receiver"));
        Long templateId = longVal(request.get("templateId"));
        Long carrierId = longVal(request.get("carrierId"));
        Long initiatorUserId = longVal(request.get("initiator_user_id"));
        Map<String, Object> variables = request.get("variables") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();

        if (channelType == null || channelType.isBlank()) {
            return ServiceResults.error(400, "channelType 不能为空");
        }
        if (receiver == null || receiver.isBlank()) {
            return ServiceResults.error(400, "receiver 不能为空");
        }
        if ("TENCENT_SMS".equals(channelType)) {
            return ServiceResults.error(501, "短信未配置");
        }

        if (templateId == null) {
            return ServiceResults.error(400, "templateId 不能为空");
        }
        MsgTemplate template = templateService.getActiveTemplate(templateId, channelType);
        if (template == null) {
            return ServiceResults.error(404, "模板不存在或未启用");
        }

        String rendered = TemplateRenderer.render(template.getContent(), variables);
        MsgCarrier carrier = "EMAIL".equals(channelType) ? carrierService.resolveCarrier(channelType, carrierId) : null;

        MsgMessage record = new MsgMessage();
        record.setTemplateId(template.getTemplateId());
        record.setCarrierId(carrier != null ? carrier.getCarrierId() : null);
        record.setInitiatorUserId(initiatorUserId);
        record.setReceiver(receiver);
        record.setChannelType(channelType);
        record.setRenderedContent(rendered);
        record.setSendTime(LocalDateTime.now());
        record.setCreatedAt(LocalDateTime.now());

        try {
            if ("EMAIL".equals(channelType)) {
                emailSendService.send(carrier, receiver, template.getName(), rendered);
            }
            record.setStatus("SUCCESS");
            messageMapper.insert(record);
            Map<String, Object> data = new HashMap<>();
            data.put("messageId", record.getMessageId());
            return ServiceResults.ok(data);
        } catch (Exception e) {
            record.setStatus("FAILED");
            record.setErrorMessage(truncate(e.getMessage(), 500));
            messageMapper.insert(record);
            return ServiceResults.error(502, "发送失败: " + e.getMessage());
        }
    }

    public Map<String, Object> getSendingRecords(Map<String, Object> params) {
        int page = intVal(params.get("page"), 1);
        int size = intVal(params.get("size"), 20);

        QueryWrapper<MsgMessage> wrapper = new QueryWrapper<>();
        if (params.get("channelType") != null) {
            wrapper.eq("channel_type", String.valueOf(params.get("channelType")));
        }
        if (params.get("receiver") != null) {
            wrapper.eq("receiver", String.valueOf(params.get("receiver")));
        }
        wrapper.orderByDesc("message_id");

        Page<MsgMessage> pageResult = messageMapper.selectPage(new Page<>(page, size), wrapper);
        List<Map<String, Object>> list = pageResult.getRecords().stream().map(this::toView).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", pageResult.getTotal());
        data.put("page", page);
        data.put("size", size);
        return ServiceResults.ok(data);
    }

    public Map<String, Object> deleteSendingRecord(Map<String, Object> request) {
        Long id = longVal(request.get("id"));
        if (id == null) {
            return ServiceResults.error(400, "id 不能为空");
        }
        if (messageMapper.deleteById(id) == 0) {
            return ServiceResults.error(404, "记录不存在");
        }
        return ServiceResults.ok();
    }

    private Map<String, Object> toView(MsgMessage message) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", message.getMessageId());
        view.put("templateId", message.getTemplateId());
        view.put("carrierId", message.getCarrierId());
        view.put("receiver", message.getReceiver());
        view.put("channelType", message.getChannelType());
        view.put("status", message.getStatus());
        view.put("sendTime", message.getSendTime());
        view.put("initiatorUserId", message.getInitiatorUserId());
        view.put("errorMessage", message.getErrorMessage());
        return view;
    }

    private String stringVal(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longVal(Object value) {
        if (value == null) {
            return null;
        }
        return Long.valueOf(String.valueOf(value));
    }

    private int intVal(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private String truncate(String message, int max) {
        if (message == null) {
            return null;
        }
        return message.length() <= max ? message : message.substring(0, max);
    }
}
