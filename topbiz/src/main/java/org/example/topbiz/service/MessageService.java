package org.example.topbiz.service;

import org.example.topbiz.feign.MessageServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 消息服务：模板、变量、载体、发送的编排
 */
@Service
public class MessageService {

    @Autowired
    private MessageServiceClient messageServiceClient;

    // ==================== 消息发送 ====================

    public Map<String, Object> sendInstant(Map<String, Object> request) {
        // TODO: 参数校验（channelType, templateId, receiver）
        return messageServiceClient.sendInstant(request);
    }

    public Map<String, Object> sendScheduled(Map<String, Object> request) {
        // TODO: 参数校验
        return messageServiceClient.sendScheduled(request);
    }

    public Map<String, Object> getSendingRecords(Map<String, Object> params) {
        return messageServiceClient.getSendingRecords(params);
    }

    public Map<String, Object> deleteSendingRecord(Map<String, Object> request) {
        return messageServiceClient.deleteSendingRecord(request);
    }

    // ==================== 消息模板 ====================

    public Map<String, Object> createTemplate(Map<String, Object> request) {
        return messageServiceClient.createTemplate(request);
    }

    public Map<String, Object> queryTemplates(Map<String, Object> params) {
        return messageServiceClient.queryTemplates(params);
    }

    public Map<String, Object> updateTemplate(Map<String, Object> request) {
        return messageServiceClient.updateTemplate(request);
    }

    // ==================== 模板变量 ====================

    public Map<String, Object> getVariableSchema() {
        return messageServiceClient.getVariableSchema();
    }

    public Map<String, Object> createVariable(Map<String, Object> request) {
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

    public Map<String, Object> createCarrier(Map<String, Object> request) {
        return messageServiceClient.createCarrier(request);
    }

    public Map<String, Object> updateCarrier(Long id, Map<String, Object> request) {
        return messageServiceClient.updateCarrier(id, request);
    }

    public Map<String, Object> deleteCarrier(Long id) {
        return messageServiceClient.deleteCarrier(id);
    }

    public Map<String, Object> testCarrier(Long id) {
        return messageServiceClient.testCarrier(id);
    }
}