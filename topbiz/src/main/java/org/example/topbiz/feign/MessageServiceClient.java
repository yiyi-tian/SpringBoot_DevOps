package org.example.topbiz.feign;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.*;

import java.util.Map;

@HttpExchange("/internal")
public interface MessageServiceClient {

    @PostExchange("/messages/instant")
    Map<String, Object> sendInstant(@RequestBody Map<String, Object> request);

    @PostExchange("/messages/scheduled")
    Map<String, Object> sendScheduled(@RequestBody Map<String, Object> request);

    @PostExchange("/message/email_code/send")
    Map<String, Object> sendEmailCode(@RequestBody Map<String, Object> request);

    @PostExchange("/message/phone_code/send")
    Map<String, Object> sendPhoneCode(@RequestBody Map<String, Object> request);

    @PostExchange("/message/verify")
    Map<String, Object> verifyCode(@RequestBody Map<String, Object> request);

    // 模板管理
    @PostExchange("/message-templates")
    Map<String, Object> createTemplate(@RequestBody Map<String, Object> request);

    @GetExchange("/message-templates")
    Map<String, Object> queryTemplates(@RequestParam Map<String, Object> params);

    @PutExchange("/message-templates")
    Map<String, Object> updateTemplate(@RequestBody Map<String, Object> request);

    @GetExchange("/message-templates/{id}")
    Map<String, Object> getTemplate(@PathVariable Long id);

    @DeleteExchange("/message-templates/{id}")
    Map<String, Object> deleteTemplate(@PathVariable Long id);
    
    // 变量管理
    @GetExchange("/variables/schema")
    Map<String, Object> getVariableSchema();

    @PostExchange("/variables")
    Map<String, Object> createVariable(@RequestBody Map<String, Object> request);

    @GetExchange("/variables/{variableId}")
    Map<String, Object> getVariable(@PathVariable String variableId);

    @PutExchange("/variables/{variableId}")
    Map<String, Object> updateVariable(@PathVariable String variableId, @RequestBody Map<String, Object> request);

    @DeleteExchange("/variables/{variableId}")
    Map<String, Object> deleteVariable(@PathVariable String variableId);

    // 载体管理
    @GetExchange("/msg/carriers")
    Map<String, Object> getCarriers(@RequestParam(required = false) String channelType);

    @GetExchange("/msg/carriers/{id}")
    Map<String, Object> getCarrier(@PathVariable Long id);

    @PostExchange("/msg/carriers")
    Map<String, Object> createCarrier(@RequestBody Map<String, Object> request);

    @PutExchange("/msg/carriers/{id}")
    Map<String, Object> updateCarrier(@PathVariable Long id, @RequestBody Map<String, Object> request);

    @DeleteExchange("/msg/carriers/{id}")
    Map<String, Object> deleteCarrier(@PathVariable Long id);

    @PostExchange("/msg/carriers/{id}/test")
    Map<String, Object> testCarrier(@PathVariable Long id);

    // 发送记录
    @GetExchange("/sending-records")
    Map<String, Object> getSendingRecords(@RequestParam Map<String, Object> params);

    @DeleteExchange("/sending-records")
    Map<String, Object> deleteSendingRecord(@RequestBody Map<String, Object> request);

    //调度
    @PostExchange("/scheduler/trigger")
    Map<String, Object> triggerScheduler();

    //信箱查询
    @GetExchange("/messages/inbox")
    Map<String, Object> getInbox(@RequestParam Map<String, Object> params);
}