package org.example.topbiz.service;

import org.example.topbiz.feign.LogServiceClient;
import org.example.topbiz.feign.UserServiceClient;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理服务：用户、分组、权限的 CRUD 编排
 */
@Service
public class AdminService {

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private LogServiceClient logServiceClient;

    // ==================== 用户管理 ====================

    public Map<String, Object> createUser(Map<String, Object> request) {
        Map<String, Object> result = userServiceClient.createUser(request);
        // TODO: 成功后记录审计日志 ADMIN_USER_CREATE
        // TODO: 发送通知消息
        return result;
    }

    public Map<String, Object> deleteUser(Map<String, Object> request) {
        Map<String, Object> result = userServiceClient.deleteUser(request);
        // TODO: 记录审计日志 ADMIN_USER_DELETE
        return result;
    }

    public Map<String, Object> updateUser(Map<String, Object> request) {
        Map<String, Object> result = userServiceClient.updateUser(request);
        // TODO: 记录审计日志 ADMIN_USER_UPDATE
        return result;
    }

    public Map<String, Object> searchUsers(Map<String, Object> params) {
        return userServiceClient.searchUsers(params);
    }

    // ==================== 分组管理 ====================

    public Map<String, Object> createGroup(Map<String, Object> request) {
        return userServiceClient.createGroup(request);
    }

    public Map<String, Object> deleteGroup(Map<String, Object> request) {
        return userServiceClient.deleteGroup(request);
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

    /**
     * 记录管理员操作审计日志
     */
    private void recordAdminAudit(Long userId, String operation, String targetId) {
        try {
            Map<String, Object> logRequest = new HashMap<>();
            logRequest.put("trace_id", MDC.get("traceId"));
            logRequest.put("user_id", userId);
            logRequest.put("operation", operation);
            logRequest.put("success", true);
            logRequest.put("target_id", targetId);
            logServiceClient.recordAudit(logRequest);
        } catch (Exception e) {
            // TODO: 记录失败日志
        }
    }
}