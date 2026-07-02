package org.example.messageservice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.messageservice.entity.*;
import org.example.messageservice.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TemplateService {

    @Autowired private MsgTemplateMapper templateMapper;
    @Autowired private MsgVariableMapper variableMapper;
    @Autowired private TemplateVariableMapper templateVariableMapper;

    public Map<String, Object> create(Map<String, Object> req) {
        String name = (String) req.get("name");
        String content = (String) req.get("content");
        String channelType = (String) req.get("channelType");
        if (name == null || name.isEmpty()) return error(400, "模板名称不能为空");
        if (content == null || content.isEmpty()) return error(400, "模板内容不能为空");
        if (channelType == null || channelType.isEmpty()) return error(400, "channelType 不能为空");

        MsgTemplate t = new MsgTemplate();
        t.setName(name); t.setContent(content); t.setChannelType(channelType);
        t.setStatus("DRAFT"); t.setCreatedAt(LocalDateTime.now()); t.setUpdatedAt(LocalDateTime.now());
        templateMapper.insert(t);

        List<Map<String, Object>> variables = (List) req.get("variables");
        if (variables != null) bindVariables(t.getTemplateId(), variables);

        return ok(Map.of("templateId", t.getTemplateId()));
    }

    public Map<String, Object> query(Map<String, Object> params) {
        int page = intVal(params.get("page"), 1), size = intVal(params.get("size"), 20);
        QueryWrapper<MsgTemplate> w = new QueryWrapper<>();
        if (params.get("channelType") != null) w.eq("channel_type", params.get("channelType"));
        if (params.get("status") != null) w.eq("status", params.get("status"));
        w.orderByDesc("created_at");
        long total = templateMapper.selectCount(w);
        w.last("LIMIT " + ((page - 1) * size) + ", " + size);
        return ok(Map.of("templates", templateMapper.selectList(w), "total", total, "page", page, "size", size));
    }

    public Map<String, Object> get(Long id) {
        MsgTemplate t = templateMapper.selectById(id);
        if (t == null) return error(404, "模板不存在");
        List<Map<String, Object>> variables = templateVariableMapper.selectList(
                new QueryWrapper<TemplateVariable>().eq("template_id", id)).stream().map(tv -> {
            MsgVariable v = variableMapper.selectById(tv.getVariableId());
            if (v == null) return null;
            Map<String, Object> m = new HashMap<>();
            m.put("variableId", v.getVariableId()); m.put("varKey", v.getVarKey());
            m.put("name", v.getName()); m.put("type", v.getType());
            m.put("required", tv.getRequiredOverride() != null ? tv.getRequiredOverride() : v.getRequired());
            m.put("defaultValue", tv.getDefaultOverride() != null ? tv.getDefaultOverride() : v.getDefaultValue());
            return m;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        return ok(Map.of("template", t, "variables", variables));
    }

    public Map<String, Object> update(Map<String, Object> req) {
        Long id = req.get("id") != null ? Long.valueOf(String.valueOf(req.get("id"))) : null;
        if (id == null) return error(400, "id 不能为空");
        MsgTemplate t = templateMapper.selectById(id);
        if (t == null) return error(404, "模板不存在");
        if (req.containsKey("name")) t.setName((String) req.get("name"));
        if (req.containsKey("content")) t.setContent((String) req.get("content"));
        if (req.containsKey("channelType")) t.setChannelType((String) req.get("channelType"));
        if (req.containsKey("status")) t.setStatus((String) req.get("status"));
        t.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(t);

        List<Map<String, Object>> variables = (List) req.get("variables");
        if (variables != null) {
            templateVariableMapper.delete(new QueryWrapper<TemplateVariable>().eq("template_id", id));
            bindVariables(id, variables);
        }
        return ok();
    }

    public Map<String, Object> delete(Long id) {
        if (templateMapper.selectById(id) == null) return error(404, "模板不存在");
        templateVariableMapper.delete(new QueryWrapper<TemplateVariable>().eq("template_id", id));
        templateMapper.deleteById(id);
        return ok();
    }

    private void bindVariables(Long templateId, List<Map<String, Object>> variables) {
        for (Map<String, Object> v : variables) {
            TemplateVariable tv = new TemplateVariable();
            tv.setTemplateId(templateId);
            tv.setVariableId(Long.valueOf(String.valueOf(v.get("variableId"))));
            if (v.containsKey("requiredOverride")) tv.setRequiredOverride(Integer.valueOf(String.valueOf(v.get("requiredOverride"))));
            if (v.containsKey("defaultOverride")) tv.setDefaultOverride((String) v.get("defaultOverride"));
            tv.setCreatedAt(LocalDateTime.now());
            templateVariableMapper.insert(tv);
        }
    }

    // ============ 模板变量 ============
    public Map<String, Object> getVariableSchema() { return ok(Map.of("types", List.of("STRING", "NUMBER", "DATE"))); }
    public Map<String, Object> createVariable(Map<String, Object> req) {
        String key = (String) req.get("varKey");
        String name = (String) req.get("name");
        if (key == null || key.isEmpty()) return error(400, "varKey 不能为空");
        if (name == null || name.isEmpty()) return error(400, "变量名不能为空");
        MsgVariable v = new MsgVariable();
        v.setVarKey(key); v.setName(name); v.setType((String) req.getOrDefault("type", "STRING"));
        v.setRequired((Integer) req.getOrDefault("required", 1));
        v.setDefaultValue((String) req.get("defaultValue"));
        v.setScope((String) req.getOrDefault("scope", "GLOBAL"));
        v.setStatus("ACTIVE"); v.setDescription((String) req.get("description"));
        v.setCreatedAt(LocalDateTime.now()); v.setUpdatedAt(LocalDateTime.now());
        variableMapper.insert(v);
        return ok(Map.of("variableId", v.getVariableId()));
    }
    public Map<String, Object> queryVariables(Map<String, Object> params) {
        return ok(Map.of("variables", variableMapper.selectList(null)));
    }
    public Map<String, Object> getVariable(String id) {
        MsgVariable v = variableMapper.selectById(Long.valueOf(id));
        return v == null ? error(404, "变量不存在") : ok(v);
    }
    public Map<String, Object> updateVariable(String id, Map<String, Object> req) {
        MsgVariable v = variableMapper.selectById(Long.valueOf(id));
        if (v == null) return error(404, "变量不存在");
        if (req.containsKey("name")) v.setName((String) req.get("name"));
        if (req.containsKey("type")) v.setType((String) req.get("type"));
        if (req.containsKey("required")) v.setRequired((Integer) req.get("required"));
        if (req.containsKey("defaultValue")) v.setDefaultValue((String) req.get("defaultValue"));
        if (req.containsKey("description")) v.setDescription((String) req.get("description"));
        v.setUpdatedAt(LocalDateTime.now());
        variableMapper.updateById(v);
        return ok();
    }
    public Map<String, Object> deleteVariable(String id) {
        variableMapper.deleteById(Long.valueOf(id));
        return ok();
    }

    private int intVal(Object v, int d) { return v == null ? d : Integer.parseInt(String.valueOf(v)); }
    private Map<String, Object> ok() { return Map.of("code", 0, "message", "ok"); }
    private Map<String, Object> ok(Object data) { return Map.of("code", 0, "message", "ok", "data", data); }
    private Map<String, Object> error(int c, String m) { return Map.of("code", c, "message", m); }
}