package org.example.topbiz.service;

import org.example.topbiz.feign.MessageServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 消息服务：模板、变量、载体、发送的编排
 */
@Service
public class MessageService {

    @Autowired
    private MessageServiceClient messageServiceClient;

    /**
     * 允许的渠道类型
     */
    private static final Set<String> VALID_CHANNEL_TYPES = new HashSet<>(Arrays.asList(
            "IN_APP", "TENCENT_SMS", "EMAIL", "FEISHU", "WECHAT"
    ));

    // ==================== 消息发送 ====================

    /**
     * 即时消息发送
     */
    public Map<String, Object> sendInstant(Map<String, Object> request) {
        // 校验 channelType
        if (!request.containsKey("channelType")) {
            throw new IllegalArgumentException("channelType 不能为空");
        }
        String channelType = String.valueOf(request.get("channelType"));
        if (!VALID_CHANNEL_TYPES.contains(channelType)) {
            throw new IllegalArgumentException("无效的 channelType: " + channelType);
        }

        // 校验 receiver
        if (!request.containsKey("receiver") || String.valueOf(request.get("receiver")).isEmpty()) {
            throw new IllegalArgumentException("receiver 不能为空");
        }

        // templateId 默认值
        if (!request.containsKey("templateId")) {
            request.put("templateId", 1); // 默认模板
        }

        return messageServiceClient.sendInstant(request);
    }

    /**
     * 定时消息发送
     */
    public Map<String, Object> sendScheduled(Map<String, Object> request) {
        // 校验 channelType
        if (!request.containsKey("channelType")) {
            throw new IllegalArgumentException("channelType 不能为空");
        }
        String channelType = String.valueOf(request.get("channelType"));
        if (!VALID_CHANNEL_TYPES.contains(channelType)) {
            throw new IllegalArgumentException("无效的 channelType: " + channelType);
        }

        // 校验 receiver
        if (!request.containsKey("receiver") || String.valueOf(request.get("receiver")).isEmpty()) {
            throw new IllegalArgumentException("receiver 不能为空");
        }

        // 校验 scheduledAt（定时发送必须有发送时间）
        if (!request.containsKey("scheduledAt")) {
            throw new IllegalArgumentException("scheduledAt 不能为空（定时发送必须指定发送时间）");
        }

        return messageServiceClient.sendScheduled(request);
    }

    /**
     * 查询发送记录
     */
    public Map<String, Object> getSendingRecords(Map<String, Object> params) {
        // 默认分页
        if (!params.containsKey("page")) {
            params.put("page", 1);
        }
        if (!params.containsKey("size")) {
            params.put("size", 20);
        }
        return messageServiceClient.getSendingRecords(params);
    }

    /**
     * 删除发送记录
     */
    public Map<String, Object> deleteSendingRecord(Map<String, Object> request) {
        if (!request.containsKey("id")) {
            throw new IllegalArgumentException("id 不能为空");
        }
        return messageServiceClient.deleteSendingRecord(request);
    }

    // ==================== 消息模板 ====================

    /**
     * 创建消息模板
     */
    public Map<String, Object> createTemplate(Map<String, Object> request) {
        // 校验必填字段
        if (!request.containsKey("name") || String.valueOf(request.get("name")).isEmpty()) {
            throw new IllegalArgumentException("模板名称不能为空");
        }
        if (!request.containsKey("content") || String.valueOf(request.get("content")).isEmpty()) {
            throw new IllegalArgumentException("模板内容不能为空");
        }
        if (!request.containsKey("channelType")) {
            throw new IllegalArgumentException("channelType 不能为空");
        }
        String channelType = String.valueOf(request.get("channelType"));
        if (!VALID_CHANNEL_TYPES.contains(channelType)) {
            throw new IllegalArgumentException("无效的 channelType: " + channelType);
        }

        // 默认状态
        if (!request.containsKey("status")) {
            request.put("status", "DRAFT");
        }

        return messageServiceClient.createTemplate(request);
    }

    /**
     * 查询模板列表
     */
    public Map<String, Object> queryTemplates(Map<String, Object> params) {
        if (!params.containsKey("page")) {
            params.put("page", 1);
        }
        if (!params.containsKey("size")) {
            params.put("size", 20);
        }
        return messageServiceClient.queryTemplates(params);
    }

    /**
     * 更新模板
     */
    public Map<String, Object> updateTemplate(Map<String, Object> request) {
        if (!request.containsKey("id")) {
            throw new IllegalArgumentException("模板 id 不能为空");
        }
        return messageServiceClient.updateTemplate(request);
    }

    // ==================== 模板变量 ====================

    public Map<String, Object> getVariableSchema() {
        return messageServiceClient.getVariableSchema();
    }

    public Map<String, Object> createVariable(Map<String, Object> request) {
        if (!request.containsKey("name") || String.valueOf(request.get("name")).isEmpty()) {
            throw new IllegalArgumentException("变量名不能为空");
        }
        return messageServiceClient.createVariable(request);
    }

    public Map<String, Object> getVariable(String variableId) {
        return messageServiceClient.getVariable(variableId);
    }

    public Map<String, Object> updateVariable(String variableId, Map<String, Object> request) {
        return messageServiceClient.updateVariable(variableId, request);
    }

    public Map<String, Object> deleteVariable(String variableId) {
        return messageServiceClient.deleteVariable(variableId);
    }

    // ==================== 载体管理 ====================

    public Map<String, Object> getCarriers(String channelType) {
        return messageServiceClient.getCarriers(channelType);
    }

    public Map<String, Object> getCarrier(Long id) {
        return messageServiceClient.getCarrier(id);
    }

    /**
     * 创建载体
     */
    public Map<String, Object> createCarrier(Map<String, Object> request) {
        // 校验必填字段
        if (!request.containsKey("name") || String.valueOf(request.get("name")).isEmpty()) {
            throw new IllegalArgumentException("载体名称不能为空");
        }
        if (!request.containsKey("channelType")) {
            throw new IllegalArgumentException("channelType 不能为空");
        }
        if (!request.containsKey("configJson")) {
            throw new IllegalArgumentException("载体配置不能为空");
        }

        String channelType = String.valueOf(request.get("channelType"));
        if (!VALID_CHANNEL_TYPES.contains(channelType)) {
            throw new IllegalArgumentException("无效的 channelType: " + channelType);
        }

        // 默认启用
        if (!request.containsKey("enabled")) {
            request.put("enabled", true);
        }

        return messageServiceClient.createCarrier(request);
    }

    public Map<String, Object> updateCarrier(Long id, Map<String, Object> request) {
        return messageServiceClient.updateCarrier(id, request);
    }

    public Map<String, Object> deleteCarrier(Long id) {
        return messageServiceClient.deleteCarrier(id);
    }

    /**
     * 载体连通性测试
     */
    public Map<String, Object> testCarrier(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("载体 id 不能为空");
        }
        return messageServiceClient.testCarrier(id);
    }

    /**
     *信箱查询 
     **/
    public Map<String, Object> getInbox(Map<String, Object> params) {
        return messageServiceClient.getInbox(params);
    }
}