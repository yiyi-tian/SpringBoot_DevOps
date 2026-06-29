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
        return ServiceResults.error(501, "定时发送未实现");
    }

    public Map<String, Object> sendEmailCode(Map<String, Object> request) {
        String email = (String) request.get("email");
        String scene = (String) request.get("scene");
        return verificationCodeService.sendEmailCode(email, scene);
    }

    public Map<String, Object> sendPhoneCode(Map<String, Object> request) {
        String phone = (String) request.get("phone");
        String scene = (String) request.get("scene");
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

    public Map<String, Object> updateTemplate(Map<String, Object> request) {
        return templateService.update(request);
    }

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

    public Map<String, Object> createVariable(Map<String, Object> request) {
        return ServiceResults.error(501, "变量 CRUD 未实现");
    }

    public Map<String, Object> getVariable(String variableId) {
        return ServiceResults.error(501, "变量 CRUD 未实现");
    }

    public Map<String, Object> updateVariable(String variableId, Map<String, Object> request) {
        return ServiceResults.error(501, "变量 CRUD 未实现");
    }

    public Map<String, Object> deleteVariable(String variableId) {
        return ServiceResults.error(501, "变量 CRUD 未实现");
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

    public Map<String, Object> getSendingRecords(Map<String, Object> params) {
        return messageDispatchService.getSendingRecords(params);
    }

    public Map<String, Object> deleteSendingRecord(Map<String, Object> request) {
        return messageDispatchService.deleteSendingRecord(request);
    }

    public Map<String, Object> triggerScheduler() {
        return ServiceResults.error(501, "调度触发未实现");
    }
}
