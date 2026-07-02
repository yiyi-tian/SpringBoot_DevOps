package org.example.topbiz.service;



import org.example.common.message.MessageConstants;

import org.example.topbiz.config.DevopsMessagingProperties;

import org.example.topbiz.exception.InternalServiceException;

import org.example.topbiz.feign.MessageServiceClient;

import org.example.topbiz.support.SecurityUtilsHelper;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;



import java.util.HashMap;

import java.util.Map;



/**

 * 消息服务：模板、变量、载体、发送的编排

 */

@Service

public class MessageService {



    @Autowired

    private MessageServiceClient messageServiceClient;

<<<<<<< HEAD
    /**
     * 允许的渠道类型
     */
    private static final Set<String> VALID_CHANNEL_TYPES = new HashSet<>(Arrays.asList(
            "IN_APP", "SMS", "EMAIL", "FEISHU", "WECHAT"
    ));
=======


    @Autowired

    private DevopsMessagingProperties messagingProperties;


>>>>>>> develop2

    // ==================== 消息发送 ====================



    public Map<String, Object> sendInstant(Map<String, Object> request) {

        String channelType = requireChannelType(request);

        validateChannelType(channelType);



        if (!request.containsKey("receiver") || String.valueOf(request.get("receiver")).isEmpty()) {

            throw new IllegalArgumentException("receiver 不能为空");

        }

        if (!request.containsKey("templateId") || String.valueOf(request.get("templateId")).isEmpty()) {

            throw new IllegalArgumentException("templateId 不能为空");

        }



        injectInitiatorUserId(request);

        return messageServiceClient.sendInstant(request);

    }



    public Map<String, Object> sendScheduled(Map<String, Object> request) {

        String channelType = requireChannelType(request);

        validateChannelType(channelType);



        if (!request.containsKey("receiver") || String.valueOf(request.get("receiver")).isEmpty()) {

            throw new IllegalArgumentException("receiver 不能为空");

        }

        if (!request.containsKey("scheduledAt")) {

            throw new IllegalArgumentException("scheduledAt 不能为空（定时发送必须指定发送时间）");

        }



        injectInitiatorUserId(request);

        return messageServiceClient.sendScheduled(request);

    }



    public Map<String, Object> getSendingRecords(Map<String, Object> params) {

        if (!params.containsKey("page")) {

            params.put("page", 1);

        }

        if (!params.containsKey("size")) {

            params.put("size", 20);

        }

        return messageServiceClient.getSendingRecords(params);

    }



    public Map<String, Object> deleteSendingRecord(Map<String, Object> request) {

        if (!request.containsKey("id")) {

            throw new IllegalArgumentException("id 不能为空");

        }

        return messageServiceClient.deleteSendingRecord(request);

    }



    // ==================== 消息模板 ====================



    public Map<String, Object> createTemplate(Map<String, Object> request) {

        if (!request.containsKey("name") || String.valueOf(request.get("name")).isEmpty()) {

            throw new IllegalArgumentException("模板名称不能为空");

        }

        if (!request.containsKey("content") || String.valueOf(request.get("content")).isEmpty()) {

            throw new IllegalArgumentException("模板内容不能为空");

        }

        String channelType = requireChannelType(request);

        validateChannelType(channelType);



        if (!request.containsKey("status")) {

            request.put("status", messagingProperties.getTemplate().getDefaultStatus());

        }



        injectInitiatorUserId(request);

        return messageServiceClient.createTemplate(request);

    }



    public Map<String, Object> queryTemplates(Map<String, Object> params) {

        if (!params.containsKey("page")) {

            params.put("page", 1);

        }

        if (!params.containsKey("size")) {

            params.put("size", 20);

        }

        return messageServiceClient.queryTemplates(params);

    }



    public Map<String, Object> updateTemplate(Map<String, Object> request) {

        if (!request.containsKey("id")) {

            throw new IllegalArgumentException("模板 id 不能为空");

        }

        return messageServiceClient.updateTemplate(request);

    }
    
    /**
     * 获取模板详情
     */
    public Map<String, Object> getTemplate(Long id) {
        return messageServiceClient.getTemplate(id);
    }

<<<<<<< HEAD
    /**
     * 删除模板
     */
    public Map<String, Object> deleteTemplate(Long id) {
        return messageServiceClient.deleteTemplate(id);
    }
    
=======


>>>>>>> develop2
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

<<<<<<< HEAD
    public Map<String, Object> queryVariables(Map<String, Object> params) {
        return messageServiceClient.queryVariables(params);
    }
=======

>>>>>>> develop2

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

        if (!request.containsKey("name") || String.valueOf(request.get("name")).isEmpty()) {

            throw new IllegalArgumentException("载体名称不能为空");

        }

        if (!request.containsKey("configJson")) {

            throw new IllegalArgumentException("载体配置不能为空");

        }

        String channelType = requireChannelType(request);

        validateChannelType(channelType);



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



    public Map<String, Object> testCarrier(Long id, Map<String, Object> request) {

        if (id == null) {

            throw new IllegalArgumentException("载体 id 不能为空");

        }

        if (request == null || !request.containsKey("testTo")

                || String.valueOf(request.get("testTo")).isBlank()) {

            throw new IllegalArgumentException("testTo 不能为空");

        }

        return messageServiceClient.testCarrier(id, request);

    }

<<<<<<< HEAD
    /**
     *信箱查询 
     **/
    public Map<String, Object> getInbox(Map<String, Object> params) {
        return messageServiceClient.getInbox(params);
    }
}
=======


    private String requireChannelType(Map<String, Object> request) {

        if (!request.containsKey("channelType")) {

            throw new IllegalArgumentException("channelType 不能为空");

        }

        return String.valueOf(request.get("channelType"));

    }



    private void validateChannelType(String channelType) {

        if (MessageConstants.isReservedChannel(channelType)) {

            throw new InternalServiceException(501, "渠道未实现: " + channelType);

        }

        if (!MessageConstants.isMvpChannel(channelType)) {

            throw new IllegalArgumentException("无效的 channelType: " + channelType);

        }

    }



    private void injectInitiatorUserId(Map<String, Object> request) {

        Long userId = SecurityUtilsHelper.getCurrentUserId();

        if (userId != null) {

            request.put("initiator_user_id", userId);

        }

    }

}

>>>>>>> develop2
