package org.example.messageservice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.common.message.MessageConstants;
import org.example.messageservice.entity.MsgTemplate;
import org.example.messageservice.mapper.MsgTemplateMapper;
import org.example.messageservice.support.ServiceResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TemplateService {

    private static final List<String> VALID_STATUS = List.of("DRAFT", "ACTIVE", "DISABLED");

    @Autowired
    private MsgTemplateMapper templateMapper;

    public Map<String, Object> create(Map<String, Object> request) {
        String name = stringVal(request.get("name"));
        String content = stringVal(request.get("content"));
        String channelType = stringVal(request.get("channelType"));
        if (name == null || name.isBlank()) {
            return ServiceResults.error(400, "name 不能为空");
        }
        if (content == null || content.isBlank()) {
            return ServiceResults.error(400, "content 不能为空");
        }
        if (channelType == null || !MessageConstants.isMvpChannel(channelType)) {
            return ServiceResults.error(400, "无效的 channelType");
        }

        MsgTemplate template = new MsgTemplate();
        template.setName(name);
        template.setContent(content);
        template.setChannelType(channelType);
        template.setStatus(stringVal(request.get("status")) != null ? stringVal(request.get("status")) : "DRAFT");
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        templateMapper.insert(template);

        Map<String, Object> data = new HashMap<>();
        data.put("templateId", template.getTemplateId());
        return ServiceResults.ok(data);
    }

    public Map<String, Object> query(Map<String, Object> params) {
        int page = intVal(params.get("page"), 1);
        int size = intVal(params.get("size"), 20);

        QueryWrapper<MsgTemplate> wrapper = new QueryWrapper<>();
        if (params.get("channelType") != null) {
            wrapper.eq("channel_type", String.valueOf(params.get("channelType")));
        }
        if (params.get("status") != null) {
            wrapper.eq("status", String.valueOf(params.get("status")));
        }
        if (params.get("keyword") != null && !String.valueOf(params.get("keyword")).isBlank()) {
            wrapper.like("name", String.valueOf(params.get("keyword")));
        }
        wrapper.orderByDesc("template_id");

        Page<MsgTemplate> pageResult = templateMapper.selectPage(new Page<>(page, size), wrapper);
        List<Map<String, Object>> list = pageResult.getRecords().stream().map(this::toView).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", pageResult.getTotal());
        data.put("page", page);
        data.put("size", size);
        return ServiceResults.ok(data);
    }

    public Map<String, Object> update(Map<String, Object> request) {
        Long id = longVal(request.get("id"));
        if (id == null) {
            return ServiceResults.error(400, "id 不能为空");
        }
        MsgTemplate template = templateMapper.selectById(id);
        if (template == null) {
            return ServiceResults.error(404, "模板不存在");
        }
        if (request.containsKey("name")) {
            template.setName(stringVal(request.get("name")));
        }
        if (request.containsKey("content")) {
            template.setContent(stringVal(request.get("content")));
        }
        if (request.containsKey("status")) {
            String status = stringVal(request.get("status"));
            if (!VALID_STATUS.contains(status)) {
                return ServiceResults.error(400, "无效的 status");
            }
            template.setStatus(status);
        }
        template.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(template);
        return ServiceResults.ok();
    }

    public MsgTemplate getActiveTemplate(Long templateId, String channelType) {
        if (templateId == null) {
            return null;
        }
        MsgTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            return null;
        }
        if (channelType != null && !channelType.equals(template.getChannelType())) {
            return null;
        }
        if (!"ACTIVE".equals(template.getStatus())) {
            return null;
        }
        return template;
    }

    private Map<String, Object> toView(MsgTemplate template) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", template.getTemplateId());
        view.put("templateId", template.getTemplateId());
        view.put("name", template.getName());
        view.put("content", template.getContent());
        view.put("channelType", template.getChannelType());
        view.put("status", template.getStatus());
        return view;
    }

    private String stringVal(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private int intVal(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private Long longVal(Object value) {
        if (value == null) {
            return null;
        }
        return Long.valueOf(String.valueOf(value));
    }
}
