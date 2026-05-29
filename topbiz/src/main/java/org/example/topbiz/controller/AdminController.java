package org.example.topbiz.controller;

import org.example.common.Result;
import org.example.topbiz.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // ==================== 用户管理 ====================
    @PostMapping("/users")
    public Result<Map<String, Object>> createUser(@RequestBody Map<String, Object> request) {
        return Result.ok(adminService.createUser(request));
    }
    @DeleteMapping("/users")
    public Result<Map<String, Object>> deleteUser(@RequestBody Map<String, Object> request) {
        return Result.ok(adminService.deleteUser(request));
    }
    @PatchMapping("/users")
    public Result<Map<String, Object>> updateUser(@RequestBody Map<String, Object> request) {
        return Result.ok(adminService.updateUser(request));
    }
    @GetMapping("/users")
    public Result<Map<String, Object>> searchUsers(@RequestParam Map<String, Object> params) {
        return Result.ok(adminService.searchUsers(params));
    }

    // ==================== 分组管理 ====================
    @PostMapping("/groups")
    public Result<Map<String, Object>> createGroup(@RequestBody Map<String, Object> request) {
        return Result.ok(adminService.createGroup(request));
    }
    @DeleteMapping("/groups")
    public Result<Map<String, Object>> deleteGroup(@RequestBody Map<String, Object> request) {
        return Result.ok(adminService.deleteGroup(request));
    }
    @PatchMapping("/groups")
    public Result<Map<String, Object>> updateGroup(@RequestBody Map<String, Object> request) {
        return Result.ok(adminService.updateGroup(request));
    }
    @GetMapping("/groups")
    public Result<Map<String, Object>> searchGroups(@RequestParam Map<String, Object> params) {
        return Result.ok(adminService.searchGroups(params));
    }

    // ==================== 用户组管理 ====================
    @PostMapping("/group-users")
    public Result<Map<String, Object>> addUserToGroup(@RequestBody Map<String, Object> request) {
        return Result.ok(adminService.addUserToGroup(request));
    }
    @DeleteMapping("/group-users")
    public Result<Map<String, Object>> removeUserFromGroup(@RequestBody Map<String, Object> request) {
        return Result.ok(adminService.removeUserFromGroup(request));
    }
    @GetMapping("/group-users")
    public Result<Map<String, Object>> searchGroupUsers(@RequestParam Map<String, Object> params) {
        return Result.ok(adminService.searchGroupUsers(params));
    }

    // ==================== 权限管理 ====================
    @PostMapping("/permissions")
    public Result<Map<String, Object>> createPermission(@RequestBody Map<String, Object> request) {
        return Result.ok(adminService.createPermission(request));
    }
    @DeleteMapping("/permissions")
    public Result<Map<String, Object>> deletePermission(@RequestBody Map<String, Object> request) {
        return Result.ok(adminService.deletePermission(request));
    }
    @PatchMapping("/permissions")
    public Result<Map<String, Object>> updatePermission(@RequestBody Map<String, Object> request) {
        return Result.ok(adminService.updatePermission(request));
    }
    @GetMapping("/permissions")
    public Result<Map<String, Object>> searchPermissions(@RequestParam Map<String, Object> params) {
        return Result.ok(adminService.searchPermissions(params));
    }

    // ==================== 分组权限管理 ====================
    @PostMapping("/group-permissions")
    public Result<Map<String, Object>> createGroupPermission(@RequestBody Map<String, Object> request) {
        return Result.ok(adminService.createGroupPermission(request));
    }
    @DeleteMapping("/group-permissions")
    public Result<Map<String, Object>> deleteGroupPermission(@RequestBody Map<String, Object> request) {
        return Result.ok(adminService.deleteGroupPermission(request));
    }
    @PatchMapping("/group-permissions")
    public Result<Map<String, Object>> updateGroupPermission(@RequestBody Map<String, Object> request) {
        return Result.ok(adminService.updateGroupPermission(request));
    }
    @GetMapping("/group-permissions")
    public Result<Map<String, Object>> searchGroupPermissions(@RequestParam Map<String, Object> params) {
        return Result.ok(adminService.searchGroupPermissions(params));
    }
}