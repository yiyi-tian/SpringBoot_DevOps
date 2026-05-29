package org.example.topbiz.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@FeignClient(name = "message-service", url = "http://localhost:8082")
public interface MessageServiceClient {

    @PostMapping("/internal/messages/instant")
    Map<String, Object> sendInstant(@RequestBody Map<String, Object> request);

    @PostMapping("/internal/messages/scheduled")
    Map<String, Object> sendScheduled(@RequestBody Map<String, Object> request);

    @PostMapping("/internal/message/email_code/send")
    Map<String, Object> sendEmailCode(@RequestBody Map<String, Object> request);

    @PostMapping("/internal/message/phone_code/send")
    Map<String, Object> sendPhoneCode(@RequestBody Map<String, Object> request);

    // 模板管理
    @PostMapping("/internal/message-templates")
    Map<String, Object> createTemplate(@RequestBody Map<String, Object> request);

    @GetMapping("/internal/message-templates")
    Map<String, Object> queryTemplates(@RequestParam Map<String, Object> params);

    @PutMapping("/internal/message-templates")
    Map<String, Object> updateTemplate(@RequestBody Map<String, Object> request);

    // 变量管理
    @GetMapping("/internal/variables/schema")
    Map<String, Object> getVariableSchema();

    @PostMapping("/internal/variables")
    Map<String, Object> createVariable(@RequestBody Map<String, Object> request);

    @GetMapping("/internal/variables/{variableId}")
    Map<String, Object> getVariable(@PathVariable String variableId);

    @PutMapping("/internal/variables/{variableId}")
    Map<String, Object> updateVariable(@PathVariable String variableId, @RequestBody Map<String, Object> request);

    @DeleteMapping("/internal/variables/{variableId}")
    Map<String, Object> deleteVariable(@PathVariable String variableId);

    // 载体管理
    @GetMapping("/internal/msg/carriers")
    Map<String, Object> getCarriers(@RequestParam(required = false) String channelType);

    @GetMapping("/internal/msg/carriers/{id}")
    Map<String, Object> getCarrier(@PathVariable Long id);

    @PostMapping("/internal/msg/carriers")
    Map<String, Object> createCarrier(@RequestBody Map<String, Object> request);

    @PutMapping("/internal/msg/carriers/{id}")
    Map<String, Object> updateCarrier(@PathVariable Long id, @RequestBody Map<String, Object> request);

    @DeleteMapping("/internal/msg/carriers/{id}")
    Map<String, Object> deleteCarrier(@PathVariable Long id);

    @PostMapping("/internal/msg/carriers/{id}/test")
    Map<String, Object> testCarrier(@PathVariable Long id);

    // 发送记录
    @GetMapping("/internal/sending-records")
    Map<String, Object> getSendingRecords(@RequestParam Map<String, Object> params);

    @DeleteMapping("/internal/sending-records")
    Map<String, Object> deleteSendingRecord(@RequestBody Map<String, Object> request);
}