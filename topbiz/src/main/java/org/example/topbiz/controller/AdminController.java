package org.example.topbiz.controller;

import org.example.common.Result;
import org.example.topbiz.service.AdminService;
import org.example.topbiz.support.ServiceResultMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // ==================== 用户管理 ====================
    @PostMapping("/users")
    public Result<Map<String, Object>> createUser(@RequestBody Map<String, Object> request) {
        return ServiceResultMapper.toResult(adminService.createUser(request));
    }

    @DeleteMapping("/users")
    public Result<Map<String, Object>> deleteUser(@RequestBody Map<String, Object> request) {
        return ServiceResultMapper.toResult(adminService.deleteUser(request));
    }

    @PatchMapping("/users")
    public Result<Map<String, Object>> updateUser(@RequestBody Map<String, Object> request) {
        return ServiceResultMapper.toResult(adminService.updateUser(request));
    }

    @GetMapping("/users")
    public Result<Map<String, Object>> searchUsers(@RequestParam Map<String, Object> params) {
        return ServiceResultMapper.toResult(adminService.searchUsers(params));
    }

    // ==================== 分组管理 ====================
    @PostMapping("/groups")
    public Result<Map<String, Object>> createGroup(@RequestBody Map<String, Object> request) {
        return ServiceResultMapper.toResult(adminService.createGroup(request));
    }

    @DeleteMapping("/groups")
    public Result<Map<String, Object>> deleteGroup(@RequestBody Map<String, Object> request) {
        return ServiceResultMapper.toResult(adminService.deleteGroup(request));
    }

    @PatchMapping("/groups")
    public Result<Map<String, Object>> updateGroup(@RequestBody Map<String, Object> request) {
        return ServiceResultMapper.toResult(adminService.updateGroup(request));
    }

    @GetMapping("/groups")
    public Result<Map<String, Object>> searchGroups(@RequestParam Map<String, Object> params) {
        return ServiceResultMapper.toResult(adminService.searchGroups(params));
    }

    // ==================== 用户组管理 ====================
    @PostMapping("/group-users")
    public Result<Map<String, Object>> addUserToGroup(@RequestBody Map<String, Object> request) {
        return ServiceResultMapper.toResult(adminService.addUserToGroup(request));
    }

    @DeleteMapping("/group-users")
    public Result<Map<String, Object>> removeUserFromGroup(@RequestBody Map<String, Object> request) {
        return ServiceResultMapper.toResult(adminService.removeUserFromGroup(request));
    }

    @GetMapping("/group-users")
    public Result<Map<String, Object>> searchGroupUsers(@RequestParam Map<String, Object> params) {
        return ServiceResultMapper.toResult(adminService.searchGroupUsers(params));
    }

    // ==================== 权限管理 ====================
    @PostMapping("/permissions")
    public Result<Map<String, Object>> createPermission(@RequestBody Map<String, Object> request) {
        return ServiceResultMapper.toResult(adminService.createPermission(request));
    }

    @DeleteMapping("/permissions")
    public Result<Map<String, Object>> deletePermission(@RequestBody Map<String, Object> request) {
        return ServiceResultMapper.toResult(adminService.deletePermission(request));
    }

    @PatchMapping("/permissions")
    public Result<Map<String, Object>> updatePermission(@RequestBody Map<String, Object> request) {
        return ServiceResultMapper.toResult(adminService.updatePermission(request));
    }

    @GetMapping("/permissions")
    public Result<Map<String, Object>> searchPermissions(@RequestParam Map<String, Object> params) {
        return ServiceResultMapper.toResult(adminService.searchPermissions(params));
    }

    // ==================== 分组权限管理 ====================
    @PostMapping("/group-permissions")
    public Result<Map<String, Object>> createGroupPermission(@RequestBody Map<String, Object> request) {
        return ServiceResultMapper.toResult(adminService.createGroupPermission(request));
    }

    @DeleteMapping("/group-permissions")
    public Result<Map<String, Object>> deleteGroupPermission(@RequestBody Map<String, Object> request) {
        return ServiceResultMapper.toResult(adminService.deleteGroupPermission(request));
    }

    @PatchMapping("/group-permissions")
    public Result<Map<String, Object>> updateGroupPermission(@RequestBody Map<String, Object> request) {
        return ServiceResultMapper.toResult(adminService.updateGroupPermission(request));
    }

    @GetMapping("/group-permissions")
    public Result<Map<String, Object>> searchGroupPermissions(@RequestParam Map<String, Object> params) {
        return ServiceResultMapper.toResult(adminService.searchGroupPermissions(params));
    }

    // ==================== 用户直接权限管理 ====================
    @PostMapping("/user-permissions")
    public Result<Map<String, Object>> createUserPermission(@RequestBody Map<String, Object> request) {
        return ServiceResultMapper.toResult(adminService.createUserPermission(request));
    }

    @DeleteMapping("/user-permissions")
    public Result<Map<String, Object>> deleteUserPermission(@RequestBody Map<String, Object> request) {
        return ServiceResultMapper.toResult(adminService.deleteUserPermission(request));
    }

    @PatchMapping("/user-permissions")
    public Result<Map<String, Object>> updateUserPermission(@RequestBody Map<String, Object> request) {
        return ServiceResultMapper.toResult(adminService.updateUserPermission(request));
    }

    @GetMapping("/user-permissions")
    public Result<Map<String, Object>> searchUserPermissions(@RequestParam Map<String, Object> params) {
        return ServiceResultMapper.toResult(adminService.searchUserPermissions(params));
    }

    // ==================== 权限申请审批 ====================

    @PostMapping("/user-permissions/{id}/approve")
    public Result<Map<String, Object>> approveUserPermission(@PathVariable Long id) {
        Map<String, Object> request = new HashMap<>();
        request.put("id", id);
        return ServiceResultMapper.toResult(adminService.approveUserPermission(request));
    }

    @PostMapping("/user-permissions/{id}/reject")
    public Result<Map<String, Object>> rejectUserPermission(@PathVariable Long id) {
        Map<String, Object> request = new HashMap<>();
        request.put("id", id);
        return ServiceResultMapper.toResult(adminService.rejectUserPermission(request));
    }

    @PostMapping("/groups/{groupId}/permissions/apply")
    public Result<Map<String, Object>> applyGroupPermission(@PathVariable Long groupId,
                                                              @RequestBody Map<String, Object> request) {
        request.put("groupId", groupId);
        return ServiceResultMapper.toResult(adminService.applyGroupPermission(request));
    }

    @PostMapping("/group-permissions/{id}/approve")
    public Result<Map<String, Object>> approveGroupPermission(@PathVariable Long id) {
        Map<String, Object> request = new HashMap<>();
        request.put("id", id);
        return ServiceResultMapper.toResult(adminService.approveGroupPermission(request));
    }

    @PostMapping("/group-permissions/{id}/reject")
    public Result<Map<String, Object>> rejectGroupPermission(@PathVariable Long id) {
        Map<String, Object> request = new HashMap<>();
        request.put("id", id);
        return ServiceResultMapper.toResult(adminService.rejectGroupPermission(request));
    }
}
