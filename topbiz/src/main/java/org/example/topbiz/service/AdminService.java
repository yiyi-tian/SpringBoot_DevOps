package org.example.topbiz.service;

import org.example.topbiz.feign.LogServiceClient;
import org.example.topbiz.feign.UserServiceClient;
import org.example.topbiz.support.SecurityUtilsHelper;
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
    private WelcomeMessageService welcomeMessageService;

    // ==================== 用户管理 ====================

    public Map<String, Object> createUser(Map<String, Object> request) {
        Map<String, Object> result = userServiceClient.createUser(request);

        if (result != null && "0".equals(String.valueOf(result.get("code")))) {
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            Long newUserId = Long.valueOf(String.valueOf(data.get("userId")));
            recordAdminAudit("ADMIN_USER_CREATE", String.valueOf(newUserId));

            welcomeMessageService.sendWelcomeMessage(newUserId);
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

    // ==================== 用户直接权限管理 ====================

    public Map<String, Object> createUserPermission(Map<String, Object> request) {
        Map<String, Object> result = userServiceClient.createUserPermission(request);
        if (result != null && "0".equals(String.valueOf(result.get("code")))) {
            String targetId = request.get("userId") + ":" + request.get("permId");
            recordAdminAudit("ADMIN_USER_PERMISSION_CREATE", targetId);
        }
        return result;
    }

    public Map<String, Object> deleteUserPermission(Map<String, Object> request) {
        Map<String, Object> result = userServiceClient.deleteUserPermission(request);
        if (result != null && "0".equals(String.valueOf(result.get("code")))) {
            String targetId = String.valueOf(request.get("id"));
            recordAdminAudit("ADMIN_USER_PERMISSION_DELETE", targetId);
        }
        return result;
    }

    public Map<String, Object> updateUserPermission(Map<String, Object> request) {
        return userServiceClient.updateUserPermission(request);
    }

    public Map<String, Object> searchUserPermissions(Map<String, Object> params) {
        return userServiceClient.searchUserPermissions(params);
    }

    // ==================== 权限申请与审批 ====================

    public Map<String, Object> applyGroupPermission(Map<String, Object> request) {
        Map<String, Object> result = userServiceClient.applyGroupPermission(request);
        if (result != null && "0".equals(String.valueOf(result.get("code")))) {
            String targetId = request.get("groupId") + ":" + request.get("permId");
            recordAdminAudit("GROUP_PERMISSION_APPLY", targetId);
        }
        return result;
    }

    public Map<String, Object> approveUserPermission(Map<String, Object> request) {
        Map<String, Object> result = userServiceClient.approveUserPermission(request);
        if (result != null && "0".equals(String.valueOf(result.get("code")))) {
            String targetId = String.valueOf(request.get("id"));
            recordAdminAudit("USER_PERMISSION_APPROVE", targetId);
        }
        return result;
    }

    public Map<String, Object> rejectUserPermission(Map<String, Object> request) {
        Map<String, Object> result = userServiceClient.rejectUserPermission(request);
        if (result != null && "0".equals(String.valueOf(result.get("code")))) {
            String targetId = String.valueOf(request.get("id"));
            recordAdminAudit("USER_PERMISSION_REJECT", targetId);
        }
        return result;
    }

    public Map<String, Object> approveGroupPermission(Map<String, Object> request) {
        Map<String, Object> result = userServiceClient.approveGroupPermission(request);
        if (result != null && "0".equals(String.valueOf(result.get("code")))) {
            String targetId = String.valueOf(request.get("id"));
            recordAdminAudit("GROUP_PERMISSION_APPROVE", targetId);
        }
        return result;
    }

    public Map<String, Object> rejectGroupPermission(Map<String, Object> request) {
        Map<String, Object> result = userServiceClient.rejectGroupPermission(request);
        if (result != null && "0".equals(String.valueOf(result.get("code")))) {
            String targetId = String.valueOf(request.get("id"));
            recordAdminAudit("GROUP_PERMISSION_REJECT", targetId);
        }
        return result;
    }

    // ==================== 私有方法 ====================

    /**
     * 获取当前登录的管理员用户ID
     */
    private Long getCurrentUserId() {
        return SecurityUtilsHelper.getCurrentUserId();
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

}