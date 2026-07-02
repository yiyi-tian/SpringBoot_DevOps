package org.example.messageservice.service;

import org.example.messageservice.support.ServiceResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息服务门面：发送、验证码、模板、变量、载体
 */
@Service
public class MessageService {

    @Autowired
    private VerificationCodeService verificationCodeService;

    @Autowired
    private MessageDispatchService messageDispatchService;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private CarrierService carrierService;

    public Map<String, Object> sendInstant(Map<String, Object> request) {
        return messageDispatchService.sendInstant(request);
    }
    public Map<String, Object> sendScheduled(Map<String, Object> request) {
        return messageDispatchService.sendScheduled(request);
    }
    public Map<String, Object> getSendingRecords(Map<String, Object> params) {
        return messageDispatchService.getSendingRecords(params);
    }
    public Map<String, Object> deleteSendingRecord(Map<String, Object> request) {
        return messageDispatchService.deleteSendingRecord(request);
    }
    public Map<String, Object> getInbox(Map<String, Object> params) {
        return messageDispatchService.getInbox(params);
    }
    public Map<String, Object> triggerScheduler() {
        return messageDispatchService.triggerScheduler();
    }

    public Map<String, Object> sendEmailCode(Map<String, Object> request) {
        String email = (String) request.get("email");
        String scene = (String) request.getOrDefault("scene", "REGISTER");
        return verificationCodeService.sendEmailCode(email, scene);
    }
    public Map<String, Object> sendPhoneCode(Map<String, Object> request) {
        String phone = (String) request.get("phone");
        String scene = (String) request.getOrDefault("scene", "REGISTER");
        return verificationCodeService.sendPhoneCode(phone, scene);
    }
    public Map<String, Object> verifyCode(Map<String, Object> request) {
        String credentialType = (String) request.get("credentialType");
        String target = (String) request.get("target");
        String scene = (String) request.get("scene");
        String code = (String) request.get("code");
        return verificationCodeService.verify(credentialType, target, scene, code);
    }
    public Map<String, Object> sendRegistrationConfirmEmail(Map<String, Object> request) {
        String email = (String) request.get("email");
        return verificationCodeService.sendRegistrationConfirmEmail(email);
    }

    public Map<String, Object> createTemplate(Map<String, Object> request) {
        return templateService.create(request);
    }
    public Map<String, Object> queryTemplates(Map<String, Object> params) {
        return templateService.query(params);
    }
    public Map<String, Object> getTemplate(Long templateId) {
        return templateService.get(templateId);
    }
    public Map<String, Object> updateTemplate(Map<String, Object> request) {
        return templateService.update(request);
    }
    public Map<String, Object> deleteTemplate(Long templateId) {
        return templateService.delete(templateId);
    }

    public Map<String, Object> getVariableSchema() {
        return templateService.getVariableSchema();
    }
    public Map<String, Object> createVariable(Map<String, Object> request) {
        return templateService.createVariable(request);
    }
    public Map<String, Object> queryVariables(Map<String, Object> params) {
        return templateService.queryVariables(params);
    }
    public Map<String, Object> getVariable(String variableId) {
        return templateService.getVariable(variableId);
    }
    public Map<String, Object> updateVariable(String variableId, Map<String, Object> request) {
        return templateService.updateVariable(variableId, request);
    }
    public Map<String, Object> deleteVariable(String variableId) {
        return templateService.deleteVariable(variableId);
    }

    public Map<String, Object> getCarriers(String channelType) {
        return carrierService.list(channelType);
    }
    public Map<String, Object> getCarrier(Long id) {
        return carrierService.get(id);
    }
    public Map<String, Object> createCarrier(Map<String, Object> request) {
        return carrierService.create(request);
    }
    public Map<String, Object> updateCarrier(Long id, Map<String, Object> request) {
        return carrierService.update(id, request);
    }
    public Map<String, Object> deleteCarrier(Long id) {
        return carrierService.delete(id);
    }
    public Map<String, Object> testCarrier(Long id, Map<String, Object> request) {
        String testTo = request != null ? (String) request.get("testTo") : null;
    return carrierService.test(id, testTo);
}
}
