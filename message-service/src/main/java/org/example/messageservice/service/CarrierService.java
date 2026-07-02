package org.example.messageservice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
from com.fasterxml.jackson.databind.ObjectMapper;
import org.example.messageservice.entity.MsgCarrier;
import org.example.messageservice.mapper.MsgCarrierMapper;
import org.example.messageservice.support.ConfigMasker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CarrierService {

    @Autowired
    private MsgCarrierMapper carrierMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> list(String channelType) {
        QueryWrapper<MsgCarrier> w = new QueryWrapper<>();
        w.isNull("deleted_at");
        if (channelType != null && !channelType.isEmpty()) w.eq("channel_type", channelType);
        List<MsgCarrier> list = carrierMapper.selectList(w);
        list.forEach(c -> c.setConfigJson(ConfigMasker.maskJson(c.getConfigJson())));
        return Map.of("code", 0, "message", "ok", "data", Map.of("carriers", list));
    }

    public Map<String, Object> get(Long id) {
        MsgCarrier c = carrierMapper.selectById(id);
        if (c == null || c.getDeletedAt() != null) return Map.of("code", 404, "message", "载体不存在");
        c.setConfigJson(ConfigMasker.maskJson(c.getConfigJson()));
        return Map.of("code", 0, "message", "ok", "data", c);
    }

    public Map<String, Object> create(Map<String, Object> req) {
        String name = (String) req.get("name");
        String channelType = (String) req.get("channelType");
        if (name == null || name.isEmpty()) return Map.of("code", 400, "message", "名称不能为空");
        if (channelType == null || channelType.isEmpty()) return Map.of("code", 400, "message", "channelType 不能为空");

        MsgCarrier c = new MsgCarrier();
        c.setName(name);
        c.setProvider((String) req.getOrDefault("provider", ""));
        c.setChannelType(channelType);
        c.setConfigJson((String) req.getOrDefault("configJson", "{}"));
        c.setEnabled(1);
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        carrierMapper.insert(c);
        return Map.of("code", 0, "message", "ok", "data", Map.of("carrierId", c.getCarrierId()));
    }

    public Map<String, Object> update(Long id, Map<String, Object> req) {
        MsgCarrier c = carrierMapper.selectById(id);
        if (c == null || c.getDeletedAt() != null) return Map.of("code", 404, "message", "载体不存在");
        if (req.containsKey("name")) c.setName((String) req.get("name"));
        if (req.containsKey("provider")) c.setProvider((String) req.get("provider"));
        if (req.containsKey("configJson")) c.setConfigJson((String) req.get("configJson"));
        if (req.containsKey("enabled")) c.setEnabled((Integer) req.get("enabled"));
        c.setUpdatedAt(LocalDateTime.now());
        carrierMapper.updateById(c);
        return Map.of("code", 0, "message", "ok");
    }

    public Map<String, Object> delete(Long id) {
        MsgCarrier c = carrierMapper.selectById(id);
        if (c == null) return Map.of("code", 404, "message", "载体不存在");
        c.setDeletedAt(LocalDateTime.now());
        carrierMapper.updateById(c);
        return Map.of("code", 0, "message", "删除成功");
    }

    public Map<String, Object> test(Long id, String testTo) {
        MsgCarrier c = carrierMapper.selectById(id);
        if (c == null || c.getDeletedAt() != null) return Map.of("code", 404, "message", "载体不存在");
        System.out.println("载体连通性测试: " + c.getName() + " channel=" + c.getChannelType());
        return Map.of("code", 0, "message", "测试成功（控制台输出）");
    }

    public MsgCarrier resolveCarrier(String channelType, Long carrierId) {
        if (carrierId != null) {
            MsgCarrier c = carrierMapper.selectById(carrierId);
            if (c != null && c.getDeletedAt() == null && c.getEnabled() == 1) return c;
        }
        QueryWrapper<MsgCarrier> w = new QueryWrapper<>();
        w.eq("channel_type", channelType).eq("enabled", 1).isNull("deleted_at").last("LIMIT 1");
        return carrierMapper.selectOne(w);
    }

        private MsgCarrier findActive(Long id) {
        if (id == null) {
            return null;
        }
        MsgCarrier carrier = carrierMapper.selectById(id);
        if (carrier == null || carrier.getDeletedAt() != null) {
            return null;
        }
        return carrier;
    }

    private Map<String, Object> toView(MsgCarrier carrier) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", carrier.getCarrierId());
        view.put("carrierId", carrier.getCarrierId());
        view.put("name", carrier.getName());
        view.put("provider", carrier.getProvider());
        view.put("channelType", carrier.getChannelType());
        view.put("enabled", carrier.getEnabled() == 1);
        if (carrier.getConfigJson() != null) {
            view.put("configJson", ConfigMasker.maskJson(carrier.getConfigJson()));
        }
        return view;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return s;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("configJson 格式无效");
        }
    }

    private String stringVal(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private int boolVal(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean b) {
            return b ? 1 : 0;
        }
        return "true".equalsIgnoreCase(String.valueOf(value)) || "1".equals(String.valueOf(value)) ? 1 : 0;
    }
}