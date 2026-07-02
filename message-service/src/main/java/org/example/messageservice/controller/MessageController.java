package org.example.messageservice.controller;

import org.example.messageservice.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.example.common.Result;
import java.util.Map;

@RestController
public class MessageController {

    @Autowired
    private MessageService messageService;

    // ==================== 消息发送 ====================
    @PostMapping("/internal/messages/instant")
    public Map<String, Object> sendInstant(@RequestBody Map<String, Object> request) {
        return messageService.sendInstant(request);
    }

    @PostMapping("/internal/messages/scheduled")
    public Map<String, Object> sendScheduled(@RequestBody Map<String, Object> request) {
        return messageService.sendScheduled(request);
    }

    // ==================== 验证码 ====================
    @PostMapping("/internal/message/email_code/send")
    public Map<String, Object> sendEmailCode(@RequestBody Map<String, Object> request) {
        return messageService.sendEmailCode(request);
    }

    @PostMapping("/internal/message/phone_code/send")
    public Map<String, Object> sendPhoneCode(@RequestBody Map<String, Object> request) {
        return messageService.sendPhoneCode(request);
    }

    @PostMapping("/internal/message/verify")
    public Map<String, Object> verifyCode(@RequestBody Map<String, Object> request) {
        return messageService.verifyCode(request);
    }

    @PostMapping("/internal/message/email/registration-confirm")
    public Map<String, Object> sendRegistrationConfirmEmail(@RequestBody Map<String, Object> request) {
        return messageService.sendRegistrationConfirmEmail(request);
    }

    // ==================== 消息模板 ====================
    @PostMapping("/internal/message-templates")
    public Map<String, Object> createTemplate(@RequestBody Map<String, Object> request) {
        return messageService.createTemplate(request);
    }

    @GetMapping("/internal/message-templates")
    public Map<String, Object> queryTemplates(@RequestParam Map<String, Object> params) {
        return messageService.queryTemplates(params);
    }

    @GetMapping("/internal/message-templates/{id}")
    public Map<String, Object> getTemplate(@PathVariable Long id) {
        return messageService.getTemplate(id);
    }

    @DeleteMapping("/internal/message-templates/{id}")
    public Map<String, Object> deleteTemplate(@PathVariable Long id) {
        return messageService.deleteTemplate(id);
    }

    @PutMapping("/internal/message-templates")
    public Map<String, Object> updateTemplate(@RequestBody Map<String, Object> request) {
        return messageService.updateTemplate(request);
    }

    // ==================== 模板变量 ====================
    @GetMapping("/internal/variables/schema")
    public Map<String, Object> getVariableSchema() {
        return messageService.getVariableSchema();
    }

    @PostMapping("/internal/variables")
    public Map<String, Object> createVariable(@RequestBody Map<String, Object> request) {
        return messageService.createVariable(request);
    }

    @GetMapping("/internal/variables")
    public Map<String, Object> queryVariables(@RequestParam Map<String, Object> params) {
        return messageService.queryVariables(params);
    }

    @GetMapping("/internal/variables/{variableId}")
    public Map<String, Object> getVariable(@PathVariable String variableId) {
        return messageService.getVariable(variableId);
    }

    @PutMapping("/internal/variables/{variableId}")
    public Map<String, Object> updateVariable(@PathVariable String variableId, @RequestBody Map<String, Object> request) {
        return messageService.updateVariable(variableId, request);
    }

    @DeleteMapping("/internal/variables/{variableId}")
    public Map<String, Object> deleteVariable(@PathVariable String variableId) {
        return messageService.deleteVariable(variableId);
    }

    // ==================== 载体管理 ====================
    @GetMapping("/internal/msg/carriers")
    public Map<String, Object> getCarriers(@RequestParam(required = false) String channelType) {
        return messageService.getCarriers(channelType);
    }

    @GetMapping("/internal/msg/carriers/{id}")
    public Map<String, Object> getCarrier(@PathVariable Long id) {
        return messageService.getCarrier(id);
    }

    @PostMapping("/internal/msg/carriers")
    public Map<String, Object> createCarrier(@RequestBody Map<String, Object> request) {
        return messageService.createCarrier(request);
    }

    @PutMapping("/internal/msg/carriers/{id}")
    public Map<String, Object> updateCarrier(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return messageService.updateCarrier(id, request);
    }

    @DeleteMapping("/internal/msg/carriers/{id}")
    public Map<String, Object> deleteCarrier(@PathVariable Long id) {
        return messageService.deleteCarrier(id);
    }

    @PostMapping("/internal/msg/carriers/{id}/test")
    public Map<String, Object> testCarrier(@PathVariable Long id,
                                           @RequestBody(required = false) Map<String, Object> request) {
        return messageService.testCarrier(id, request);
    }

    // ==================== 发送记录 ====================
    @GetMapping("/internal/sending-records")
    public Map<String, Object> getSendingRecords(@RequestParam Map<String, Object> params) {
        return messageService.getSendingRecords(params);
    }

    @DeleteMapping("/internal/sending-records")
    public Map<String, Object> deleteSendingRecord(@RequestBody Map<String, Object> request) {
        return messageService.deleteSendingRecord(request);
    }

    // ==================== 调度触发 ====================
    @PostMapping("/internal/scheduler/trigger")
    public Map<String, Object> triggerScheduler() {
        return messageService.triggerScheduler();
    }

    // ==================== 信箱查询 ====================
    @GetMapping("/internal/messages/inbox")
    public Result<Map<String, Object>> getInbox(@RequestParam Map<String, Object> params) {
        Map<String, Object> result = messageService.getInbox(params);
        // message-service 返回 {code:0, data:{...}}，直接取 data 包装
        if (result != null && "0".equals(String.valueOf(result.get("code")))) {
            return Result.ok((Map<String, Object>) result.get("data"));
        }
        return Result.error(500, "查询失败");
    }
}