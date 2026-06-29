package org.example.topbiz.controller;

import org.example.common.Result;
import org.example.topbiz.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class MessageController {

    @Autowired
    private MessageService messageService;

    // ==================== 消息发送 ====================
    @PostMapping("/send/instant")
    public Result<Map<String, Object>> sendInstant(@RequestBody Map<String, Object> request) {
        return Result.ok(messageService.sendInstant(request));
    }
    @PostMapping("/send/scheduled")
    public Result<Map<String, Object>> sendScheduled(@RequestBody Map<String, Object> request) {
        return Result.ok(messageService.sendScheduled(request));
    }
    @GetMapping("/sending-records")
    public Result<Map<String, Object>> getSendingRecords(@RequestParam Map<String, Object> params) {
        return Result.ok(messageService.getSendingRecords(params));
    }
    @DeleteMapping("/sending-records/{id}")
    public Result<Map<String, Object>> deleteSendingRecord(@PathVariable Long id) {
        return Result.ok(messageService.deleteSendingRecord(Map.of("id", id)));
    }

    // ==================== 消息模板 ====================
    @PostMapping("/templates")
    public Result<Map<String, Object>> createTemplate(@RequestBody Map<String, Object> request) {
        return Result.ok(messageService.createTemplate(request));
    }
    @GetMapping("/templates")
    public Result<Map<String, Object>> queryTemplates(@RequestParam Map<String, Object> params) {
        return Result.ok(messageService.queryTemplates(params));
    }
    @PutMapping("/templates/{id}/status")
    public Result<Map<String, Object>> updateTemplateStatus(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        request.put("id", id);
        return Result.ok(messageService.updateTemplate(request));
    }
    @GetMapping("/templates/{id}")
    public Result<Map<String, Object>> getTemplate(@PathVariable Long id) {
        return Result.ok(messageService.getTemplate(id));
    }

    @DeleteMapping("/templates/{id}")
    public Result<Map<String, Object>> deleteTemplate(@PathVariable Long id) {
        return Result.ok(messageService.deleteTemplate(id));
    }
    
    // ==================== 模板变量 ====================
    @GetMapping("/variables/schema")
    public Result<Map<String, Object>> getVariableSchema() {
        return Result.ok(messageService.getVariableSchema());
    }
    @PostMapping("/variables")
    public Result<Map<String, Object>> createVariable(@RequestBody Map<String, Object> request) {
        return Result.ok(messageService.createVariable(request));
    }
    @GetMapping("/variables/{variableId}")
    public Result<Map<String, Object>> getVariable(@PathVariable String variableId) {
        return Result.ok(messageService.getVariable(variableId));
    }
    @PutMapping("/variables/{variableId}")
    public Result<Map<String, Object>> updateVariable(@PathVariable String variableId, @RequestBody Map<String, Object> request) {
        return Result.ok(messageService.updateVariable(variableId, request));
    }
    @DeleteMapping("/variables/{variableId}")
    public Result<Map<String, Object>> deleteVariable(@PathVariable String variableId) {
        return Result.ok(messageService.deleteVariable(variableId));
    }

    // ==================== 载体管理 ====================
    @GetMapping("/msg/carriers")
    public Result<Map<String, Object>> getCarriers(@RequestParam(required = false) String channelType) {
        return Result.ok(messageService.getCarriers(channelType));
    }
    @GetMapping("/msg/carriers/{id}")
    public Result<Map<String, Object>> getCarrier(@PathVariable Long id) {
        return Result.ok(messageService.getCarrier(id));
    }
    @PostMapping("/msg/carriers")
    public Result<Map<String, Object>> createCarrier(@RequestBody Map<String, Object> request) {
        return Result.ok(messageService.createCarrier(request));
    }
    @PutMapping("/msg/carriers/{id}")
    public Result<Map<String, Object>> updateCarrier(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return Result.ok(messageService.updateCarrier(id, request));
    }
    @DeleteMapping("/msg/carriers/{id}")
    public Result<Map<String, Object>> deleteCarrier(@PathVariable Long id) {
        return Result.ok(messageService.deleteCarrier(id));
    }
    @PostMapping("/msg/carriers/{id}/test")
    public Result<Map<String, Object>> testCarrier(@PathVariable Long id) {
        return Result.ok(messageService.testCarrier(id));
    }
    
    @GetMapping("/messages/inbox")
    public Result<Map<String, Object>> getInbox(@RequestParam Map<String, Object> params) {
        Map<String, Object> result = messageService.getInbox(params);
        if (result != null && "0".equals(String.valueOf(result.get("code")))) {
            return Result.ok((Map<String, Object>) result.get("data"));
        }
        return Result.error(500, "查询失败");
    }
}