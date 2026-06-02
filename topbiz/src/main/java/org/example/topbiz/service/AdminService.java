package org.example.topbiz.service;

import org.apache.shiro.SecurityUtils;
import org.example.topbiz.feign.LogServiceClient;
import org.example.topbiz.feign.MessageServiceClient;
import org.example.topbiz.feign.UserServiceClient;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AdminService {

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private LogServiceClient logServiceClient;

    @Autowired
    private MessageServiceClient messageServiceClient;

    // ==================== 用户管理 ====================

    public Map<String, Object> createUser(Map<String, Object> request) {
        Map<String, Object> result = userServiceClient.createUser(request);

        if (result != null && "0".equals(String.valueOf(result.get("code")))) {
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            Long newUserId = Long.valueOf(String.valueOf(data.get("userId")));
            recordAdminAudit("ADMIN_USER_CREATE", String.valueOf(newUserId));

            sendWelcomeMessage(newUserId);
        }
        return result;
    }

    public Map<String, Object> deleteUser(Map<String, Object> request) {
        Map<String, Object> result = userServiceClient.deleteUser(request);

        if (result != null && "0".equals(String.valueOf(result.get("code")))) {
            Object userIdObj = request.get("userId");
            String targetId = userIdObj != null ? String.valueOf(userIdObj) : null;
            recordAdminAudit("ADMIN_USER_DELETE", targetId);
        }
        return result;
    }

    public Map<String, Object> updateUser(Map<String, Object> request) {
        Map<String, Object> result = userServiceClient.updateUser(request);

        if (result != null && "0".equals(String.valueOf(result.get("code")))) {
            Object userIdObj = request.get("userId");
            String targetId = userIdObj != null ? String.valueOf(userIdObj) : null;
            recordAdminAudit("ADMIN_USER_UPDATE", targetId);
        }
        return result;
    }

    public Map<String, Object> searchUsers(Map<String, Object> params) {
        return userServiceClient.searchUsers(params);
    }

    // ==================== 分组管理 ====================

    public Map<String, Object> createGroup(Map<String, Object> request) {
        Map<String, Object> result = userServiceClient.createGroup(request);
        if (result != null && "0".equals(String.valueOf(result.get("code")))) {
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            String groupId = data != null ? String.valueOf(data.get("groupId")) : null;
            recordAdminAudit("ADMIN_GROUP_CREATE", groupId);
        }
        return result;
    }

    public Map<String, Object> deleteGroup(Map<String, Object> request) {
        Map<String, Object> result = userServiceClient.deleteGroup(request);
        if (result != null && "0".equals(String.valueOf(result.get("code")))) {
            String groupId = String.valueOf(request.get("groupId"));
            recordAdminAudit("ADMIN_GROUP_DELETE", groupId);
        }
        return result;
    }

    public Map<String, Object> updateGroup(Map<String, Object> request) {
        return userServiceClient.updateGroup(request);
    }

    public Map<String, Object> searchGroups(Map<String, Object> params) {
        return userServiceClient.searchGroups(params);
    }

    // ==================== 用户组管理 ====================

    public Map<String, Object> addUserToGroup(Map<String, Object> request) {
        return userServiceClient.addUserToGroup(request);
    }

    public Map<String, Object> removeUserFromGroup(Map<String, Object> request) {
        return userServiceClient.removeUserFromGroup(request);
    }

    public Map<String, Object> searchGroupUsers(Map<String, Object> params) {
        return userServiceClient.searchGroupUsers(params);
    }

    // ==================== 权限管理 ====================

    public Map<String, Object> createPermission(Map<String, Object> request) {
        return userServiceClient.createPermission(request);
    }

    public Map<String, Object> deletePermission(Map<String, Object> request) {
        return userServiceClient.deletePermission(request);
    }

    public Map<String, Object> updatePermission(Map<String, Object> request) {
        return userServiceClient.updatePermission(request);
    }

    public Map<String, Object> searchPermissions(Map<String, Object> params) {
        return userServiceClient.searchPermissions(params);
    }

    // ==================== 分组权限管理 ====================

    public Map<String, Object> createGroupPermission(Map<String, Object> request) {
        return userServiceClient.createGroupPermission(request);
    }

    public Map<String, Object> deleteGroupPermission(Map<String, Object> request) {
        return userServiceClient.deleteGroupPermission(request);
    }

    public Map<String, Object> updateGroupPermission(Map<String, Object> request) {
        return userServiceClient.updateGroupPermission(request);
    }

    public Map<String, Object> searchGroupPermissions(Map<String, Object> params) {
        return userServiceClient.searchGroupPermissions(params);
    }

    // ==================== 私有方法 ====================

    /**
     * 获取当前登录的管理员用户ID
     */
    private Long getCurrentUserId() {
        try {
            String userId = (String) SecurityUtils.getSubject().getPrincipal();
            return userId != null ? Long.valueOf(userId) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 记录管理员操作审计日志
     * @param operation 操作类型
     * @param targetId 操作目标ID
     */
    private void recordAdminAudit(String operation, String targetId) {
        try {
            Long userId = getCurrentUserId();
            Map<String, Object> logRequest = new HashMap<>();
            logRequest.put("trace_id", MDC.get("traceId"));
            logRequest.put("user_id", userId);
            logRequest.put("operation", operation);
            logRequest.put("success", true);
            logRequest.put("target_id", targetId);
            logServiceClient.recordAudit(logRequest);
        } catch (Exception e) {
            System.err.println("记录审计日志失败: " + e.getMessage());
        }
    }

    /**
     * 发送欢迎站内信
     */
    private void sendWelcomeMessage(Long userId) {
        try {
            Map<String, Object> msgRequest = new HashMap<>();
            msgRequest.put("channelType", "IN_APP");
            msgRequest.put("templateId", 1);
            msgRequest.put("receiver", String.valueOf(userId));
            msgRequest.put("variables", new HashMap<>());
            messageServiceClient.sendInstant(msgRequest);
        } catch (Exception e) {
            System.err.println("发送欢迎信失败: " + e.getMessage());
        }
    }
}