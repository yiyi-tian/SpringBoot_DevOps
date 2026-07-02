package org.example.messageservice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.common.message.MessageConstants;
import org.example.messageservice.entity.MsgTemplate;
import org.example.messageservice.mapper.MsgTemplateMapper;
import org.example.messageservice.entity.MsgVariable;
import org.example.messageservice.mapper.MsgVariableMapper;
import org.example.messageservice.entity.TemplateVariable;
import org.example.messageservice.mapper.TemplateVariableMapper;
import org.example.messageservice.support.ServiceResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TemplateService {

    private static final List<String> VALID_STATUS = List.of("DRAFT", "ACTIVE", "DISABLED");

    @Autowired
    private MsgTemplateMapper templateMapper;

    @Autowired
    private MsgVariableMapper variableMapper;

    @Autowired
    private TemplateVariableMapper templateVariableMapper;

    // ==================== 模板 CRUD ====================

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

        // 幂等：检查是否已存在同名同渠道模板
        QueryWrapper<MsgTemplate> existWrapper = new QueryWrapper<>();
        existWrapper.eq("name", name).eq("channel_type", channelType);
        if (templateMapper.selectCount(existWrapper) > 0) {
            return ServiceResults.error(409, "模板已存在");
        }

        MsgTemplate template = new MsgTemplate();
        template.setName(name);
        template.setContent(content);
        template.setChannelType(channelType);
        template.setStatus(stringVal(request.get("status")) != null ? stringVal(request.get("status")) : "DRAFT");
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        templateMapper.insert(template);

        List<Map<String, Object>> variables = (List) request.get("variables");
        if (variables != null) bindVariables(template.getTemplateId(), variables);

        return ServiceResults.ok(Map.of("templateId", template.getTemplateId()));
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
        return ServiceResults.ok(Map.of("list", list, "total", pageResult.getTotal(), "page", page, "size", size));
    }

    public Map<String, Object> get(Long templateId) {
        MsgTemplate template = templateMapper.selectById(templateId);
        if (template == null) return ServiceResults.error(404, "模板不存在");

        List<Map<String, Object>> variables = templateVariableMapper.selectList(
                new QueryWrapper<TemplateVariable>().eq("template_id", templateId)).stream().map(tv -> {
            MsgVariable v = variableMapper.selectById(tv.getVariableId());
            if (v == null) return null;
            Map<String, Object> m = new HashMap<>();
            m.put("variableId", v.getVariableId()); m.put("varKey", v.getVarKey());
            m.put("name", v.getName()); m.put("type", v.getType());
            m.put("required", tv.getRequiredOverride() != null ? tv.getRequiredOverride() : v.getRequired());
            m.put("defaultValue", tv.getDefaultOverride() != null ? tv.getDefaultOverride() : v.getDefaultValue());
            return m;
        }).filter(Objects::nonNull).collect(Collectors.toList());

        return ServiceResults.ok(Map.of("template", toView(template), "variables", variables));
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

        // 幂等：如果改名，检查新名字是否与其他模板冲突
        if (request.containsKey("name")) {
            String newName = stringVal(request.get("name"));
            if (!newName.equals(template.getName())) {
                QueryWrapper<MsgTemplate> dupWrapper = new QueryWrapper<>();
                dupWrapper.eq("name", newName)
                        .eq("channel_type", template.getChannelType())
                        .ne("template_id", id);
                if (templateMapper.selectCount(dupWrapper) > 0) {
                    return ServiceResults.error(409, "模板名称已存在");
                }
            }
            template.setName(newName);
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

        List<Map<String, Object>> variables = (List) request.get("variables");
        if (variables != null) {
            templateVariableMapper.delete(new QueryWrapper<TemplateVariable>().eq("template_id", id));
            bindVariables(id, variables);
        }
        return ServiceResults.ok();
    }

    public Map<String, Object> delete(Long templateId) {
        if (templateMapper.selectById(templateId) == null) return ServiceResults.error(404, "模板不存在");
        templateVariableMapper.delete(new QueryWrapper<TemplateVariable>().eq("template_id", templateId));
        templateMapper.deleteById(templateId);
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

    // ==================== 模板变量 CRUD ====================

    public Map<String, Object> getVariableSchema() { 
        Map<String, Object> schema = new HashMap<>();
        schema.put("rules", List.of(
                Map.of("name", "code", "required", true, "description", "验证码，6 位数字"),
                Map.of("name", "username", "required", false, "description", "用户名"),
                Map.of("name", "link", "required", false, "description", "链接 URL")
        ));
        schema.put("placeholderFormat", "${varName}");
        return ServiceResults.ok(schema);
    }

    public Map<String, Object> createVariable(Map<String, Object> req) {
        String key = stringVal(req.get("varKey"));
        String name = stringVal(req.get("name"));
        if (key == null || key.isEmpty()) return ServiceResults.error(400, "varKey 不能为空");
        if (name == null || name.isEmpty()) return ServiceResults.error(400, "变量名不能为空");

        // 幂等：检查 varKey 是否已存在
        QueryWrapper<MsgVariable> existWrapper = new QueryWrapper<>();
        existWrapper.eq("var_key", key);
        if (variableMapper.selectCount(existWrapper) > 0) {
            return ServiceResults.error(409, "变量已存在");
        }

        MsgVariable v = new MsgVariable();
        v.setVarKey(key); v.setName(name);
        v.setType(stringVal(req.get("type")) != null ? stringVal(req.get("type")) : "STRING");
        v.setRequired((Integer) req.getOrDefault("required", 1));
        v.setDefaultValue(stringVal(req.get("defaultValue")));
        v.setScope(stringVal(req.get("scope")) != null ? stringVal(req.get("scope")) : "GLOBAL");
        v.setStatus("ACTIVE"); v.setDescription(stringVal(req.get("description")));
        v.setCreatedAt(LocalDateTime.now()); v.setUpdatedAt(LocalDateTime.now());
        variableMapper.insert(v);
        return ServiceResults.ok(Map.of("variableId", v.getVariableId()));
    }

    public Map<String, Object> queryVariables(Map<String, Object> params) {
        return ServiceResults.ok(Map.of("variables", variableMapper.selectList(null)));
    }

    public Map<String, Object> getVariable(String id) {
        MsgVariable v = variableMapper.selectById(Long.valueOf(id));
        return v == null ? ServiceResults.error(404, "变量不存在") : ServiceResults.ok(v);
    }

    public Map<String, Object> updateVariable(String id, Map<String, Object> req) {
        MsgVariable v = variableMapper.selectById(Long.valueOf(id));
        if (v == null) return ServiceResults.error(404, "变量不存在");

        // 幂等：如果改了 varKey，检查新 key 是否冲突
        if (req.containsKey("varKey")) {
            String newKey = stringVal(req.get("varKey"));
            if (!newKey.equals(v.getVarKey())) {
                QueryWrapper<MsgVariable> dupWrapper = new QueryWrapper<>();
                dupWrapper.eq("var_key", newKey).ne("variable_id", v.getVariableId());
                if (variableMapper.selectCount(dupWrapper) > 0) {
                    return ServiceResults.error(409, "变量 key 已存在");
                }
            }
            v.setVarKey(newKey);
        }
        if (req.containsKey("name")) v.setName(stringVal(req.get("name")));
        if (req.containsKey("type")) v.setType(stringVal(req.get("type")));
        if (req.containsKey("required")) v.setRequired((Integer) req.get("required"));
        if (req.containsKey("defaultValue")) v.setDefaultValue(stringVal(req.get("defaultValue")));
        if (req.containsKey("description")) v.setDescription(stringVal(req.get("description")));
        v.setUpdatedAt(LocalDateTime.now());
        variableMapper.updateById(v);
        return ServiceResults.ok();
    }

    public Map<String, Object> deleteVariable(String id) {
        variableMapper.deleteById(Long.valueOf(id));
        return ServiceResults.ok();
    }

    // ==================== 私有辅助 ====================

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
