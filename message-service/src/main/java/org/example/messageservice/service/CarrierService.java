package org.example.messageservice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.common.message.MessageConstants;
import org.example.messageservice.entity.MsgCarrier;
import org.example.messageservice.mapper.MsgCarrierMapper;
import org.example.messageservice.support.ConfigMasker;
import org.example.messageservice.support.ServiceResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CarrierService {

    @Autowired
    private MsgCarrierMapper carrierMapper;

    @Autowired
    private EmailSendService emailSendService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> list(String channelType) {
        QueryWrapper<MsgCarrier> wrapper = new QueryWrapper<>();
        wrapper.isNull("deleted_at");
        if (channelType != null && !channelType.isBlank()) {
            wrapper.eq("channel_type", channelType);
        }
        wrapper.orderByDesc("carrier_id");
        List<Map<String, Object>> list = carrierMapper.selectList(wrapper).stream()
                .map(this::toView)
                .collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", list.size());
        return ServiceResults.ok(data);
    }

    public Map<String, Object> get(Long id) {
        MsgCarrier carrier = findActive(id);
        if (carrier == null) {
            return ServiceResults.error(404, "载体不存在");
        }
        return ServiceResults.ok(toView(carrier));
    }

    public Map<String, Object> create(Map<String, Object> request) {
        String name = stringVal(request.get("name"));
        String channelType = stringVal(request.get("channelType"));
        if (name == null || name.isBlank()) {
            return ServiceResults.error(400, "name 不能为空");
        }
        if (channelType == null || !MessageConstants.isMvpChannel(channelType)) {
            return ServiceResults.error(400, "无效的 channelType");
        }

        // 幂等：检查是否已存在同名同渠道载体
        QueryWrapper<MsgCarrier> existWrapper = new QueryWrapper<>();
        existWrapper.eq("name", name).eq("channel_type", channelType).isNull("deleted_at");
        if (carrierMapper.selectCount(existWrapper) > 0) {
            return ServiceResults.error(409, "载体已存在");
        }

        MsgCarrier carrier = new MsgCarrier();
        carrier.setName(name);
        carrier.setChannelType(channelType);
        carrier.setProvider(stringVal(request.get("provider")));
        carrier.setConfigJson(toJson(request.get("configJson")));
        carrier.setEnabled(boolVal(request.get("enabled"), 1));
        carrier.setCreatedAt(LocalDateTime.now());
        carrier.setUpdatedAt(LocalDateTime.now());
        carrierMapper.insert(carrier);

        return ServiceResults.ok(Map.of("carrierId", carrier.getCarrierId()));
    }

    public Map<String, Object> update(Long id, Map<String, Object> request) {
        MsgCarrier carrier = findActive(id);
        if (carrier == null) {
            return ServiceResults.error(404, "载体不存在");
        }

        // 幂等：如果改名，检查新名字是否与其他载体冲突
        if (request.containsKey("name")) {
            String newName = stringVal(request.get("name"));
            if (!newName.equals(carrier.getName())) {
                QueryWrapper<MsgCarrier> dupWrapper = new QueryWrapper<>();
                dupWrapper.eq("name", newName)
                        .eq("channel_type", carrier.getChannelType())
                        .isNull("deleted_at")
                        .ne("carrier_id", id);
                if (carrierMapper.selectCount(dupWrapper) > 0) {
                    return ServiceResults.error(409, "载体名称已存在");
                }
            }
            carrier.setName(newName);
        }
        if (request.containsKey("provider")) {
            carrier.setProvider(stringVal(request.get("provider")));
        }
        if (request.containsKey("configJson")) {
            carrier.setConfigJson(toJson(request.get("configJson")));
        }
        if (request.containsKey("enabled")) {
            carrier.setEnabled(boolVal(request.get("enabled"), carrier.getEnabled()));
        }
        carrier.setUpdatedAt(LocalDateTime.now());
        carrierMapper.updateById(carrier);
        return ServiceResults.ok();
    }

    public Map<String, Object> delete(Long id) {
        MsgCarrier carrier = findActive(id);
        if (carrier == null) {
            return ServiceResults.error(404, "载体不存在");
        }
        carrier.setDeletedAt(LocalDateTime.now());
        carrier.setEnabled(0);
        carrier.setUpdatedAt(LocalDateTime.now());
        carrierMapper.updateById(carrier);
        return ServiceResults.ok();
    }

    public Map<String, Object> test(Long id, String testTo) {
        MsgCarrier carrier = findActive(id);
        if (carrier == null) {
            return ServiceResults.error(404, "载体不存在");
        }
        if (!"EMAIL".equals(carrier.getChannelType())) {
            return ServiceResults.error(400, "当前仅支持 EMAIL 载体测试");
        }
        if (testTo == null || testTo.isBlank()) {
            return ServiceResults.error(400, "testTo 不能为空");
        }
        try {
            emailSendService.send(carrier, testTo, "DevOps 载体连通性测试", "这是一封测试邮件，说明 SMTP 配置可用。");
            return ServiceResults.ok(Map.of("success", true));
        } catch (Exception e) {
            return ServiceResults.error(502, e.getMessage());
        }
    }

    public MsgCarrier resolveCarrier(String channelType, Long carrierId) {
        if (carrierId != null) {
            MsgCarrier carrier = findActive(carrierId);
            if (carrier != null && channelType.equals(carrier.getChannelType()) && carrier.getEnabled() == 1) {
                return carrier;
            }
            return null;
        }
        QueryWrapper<MsgCarrier> wrapper = new QueryWrapper<>();
        wrapper.eq("channel_type", channelType);
        wrapper.eq("enabled", 1);
        wrapper.isNull("deleted_at");
        wrapper.orderByAsc("carrier_id");
        wrapper.last("LIMIT 1");
        return carrierMapper.selectOne(wrapper);
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
