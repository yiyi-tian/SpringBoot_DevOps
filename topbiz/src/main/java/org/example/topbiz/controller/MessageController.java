package org.example.topbiz.controller;

import org.example.common.Result;
import org.example.topbiz.service.MessageService;
import org.example.topbiz.support.ServiceResultMapper;
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
        return ServiceResultMapper.toResult(messageService.sendInstant(request));
    }

    @PostMapping("/send/scheduled")
    public Result<Map<String, Object>> sendScheduled(@RequestBody Map<String, Object> request) {
        return ServiceResultMapper.toResult(messageService.sendScheduled(request));
    }

    @GetMapping("/sending-records")
    public Result<Map<String, Object>> getSendingRecords(@RequestParam Map<String, Object> params) {
        return ServiceResultMapper.toResult(messageService.getSendingRecords(params));
    }

    @DeleteMapping("/sending-records/{id}")
    public Result<Map<String, Object>> deleteSendingRecord(@PathVariable Long id) {
        return ServiceResultMapper.toResult(messageService.deleteSendingRecord(Map.of("id", id)));
    }

    // ==================== 消息模板 ====================
    @PostMapping("/templates")
    public Result<Map<String, Object>> createTemplate(@RequestBody Map<String, Object> request) {
        return ServiceResultMapper.toResult(messageService.createTemplate(request));
    }

    @GetMapping("/templates")
    public Result<Map<String, Object>> queryTemplates(@RequestParam Map<String, Object> params) {
        return ServiceResultMapper.toResult(messageService.queryTemplates(params));
    }

    @PutMapping("/templates/{id}/status")
    public Result<Map<String, Object>> updateTemplateStatus(@PathVariable Long id,
                                                            @RequestBody Map<String, Object> request) {
        request.put("id", id);
        return ServiceResultMapper.toResult(messageService.updateTemplate(request));
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
        return ServiceResultMapper.toResult(messageService.getVariableSchema());
    }

    @PostMapping("/variables")
    public Result<Map<String, Object>> createVariable(@RequestBody Map<String, Object> request) {
        return ServiceResultMapper.toResult(messageService.createVariable(request));
    }
    @GetMapping("/variables")
    public Result<Map<String, Object>> queryVariables(@RequestParam Map<String, Object> params) {
        return Result.ok(messageService.queryVariables(params));
    }

    @GetMapping("/variables/{variableId}")
    public Result<Map<String, Object>> getVariable(@PathVariable String variableId) {
        return ServiceResultMapper.toResult(messageService.getVariable(variableId));
    }

    @PutMapping("/variables/{variableId}")
    public Result<Map<String, Object>> updateVariable(@PathVariable String variableId,
                                                      @RequestBody Map<String, Object> request) {
        return ServiceResultMapper.toResult(messageService.updateVariable(variableId, request));
    }

    @DeleteMapping("/variables/{variableId}")
    public Result<Map<String, Object>> deleteVariable(@PathVariable String variableId) {
        return ServiceResultMapper.toResult(messageService.deleteVariable(variableId));
    }

    // ==================== 载体管理 ====================
    @GetMapping("/msg/carriers")
    public Result<Map<String, Object>> getCarriers(@RequestParam(required = false) String channelType) {
        return ServiceResultMapper.toResult(messageService.getCarriers(channelType));
    }

    @GetMapping("/msg/carriers/{id}")
    public Result<Map<String, Object>> getCarrier(@PathVariable Long id) {
        return ServiceResultMapper.toResult(messageService.getCarrier(id));
    }

    @PostMapping("/msg/carriers")
    public Result<Map<String, Object>> createCarrier(@RequestBody Map<String, Object> request) {
        return ServiceResultMapper.toResult(messageService.createCarrier(request));
    }

    @PutMapping("/msg/carriers/{id}")
    public Result<Map<String, Object>> updateCarrier(@PathVariable Long id,
                                                     @RequestBody Map<String, Object> request) {
        return ServiceResultMapper.toResult(messageService.updateCarrier(id, request));
    }

    @DeleteMapping("/msg/carriers/{id}")
    public Result<Map<String, Object>> deleteCarrier(@PathVariable Long id) {
        return ServiceResultMapper.toResult(messageService.deleteCarrier(id));
    }

    @PostMapping("/msg/carriers/{id}/test")
    public Result<Map<String, Object>> testCarrier(@PathVariable Long id,
                                                   @RequestBody(required = false) Map<String, Object> request) {
        return ServiceResultMapper.toResult(messageService.testCarrier(id, request));
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
