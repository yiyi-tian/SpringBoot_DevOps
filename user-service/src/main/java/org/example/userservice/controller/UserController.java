package org.example.userservice.controller;

import org.example.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/internal/user/register")
    public Map<String, Object> register(@RequestBody Map<String, Object> request) {
        return userService.register(request);
    }

    @PostMapping("/internal/user/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> request) {
        return userService.login(request);
    }

    @PostMapping("/internal/user/{userId}/deregister")
    public Map<String, Object> deregister(@PathVariable Long userId) {
        return userService.deregister(userId);
    }

    @PostMapping("/internal/user/{userId}/logout")
    public Map<String, Object> logout(@PathVariable Long userId) {
        return userService.logout(userId);
    }

    @GetMapping("/internal/user/{userId}/permissions")
    public Map<String, Object> getPermissions(@PathVariable Long userId) {
        return userService.getPermissions(userId);
    }

    @GetMapping("/internal/user/{userId}/groups")
    public Map<String, Object> getGroups(@PathVariable Long userId) {
        return userService.getGroups(userId);
    }

    // ==================== 管理员接口 ====================

    @PostMapping("/internal/user/create")
    public Map<String, Object> createUser(@RequestBody Map<String, Object> request) {
        return userService.createUser(request);
    }

    @DeleteMapping("/internal/user/delete")
    public Map<String, Object> deleteUser(@RequestBody Map<String, Object> request) {
        return userService.deleteUser(request);
    }

    @PatchMapping("/internal/user/update")
    public Map<String, Object> updateUser(@RequestBody Map<String, Object> request) {
        return userService.updateUser(request);
    }

    @GetMapping("/internal/user/search")
    public Map<String, Object> searchUsers(@RequestParam Map<String, Object> params) {
        return userService.searchUsers(params);
    }

    // ==================== 分组管理 ====================

    @PostMapping("/internal/group/create")
    public Map<String, Object> createGroup(@RequestBody Map<String, Object> request) {
        return userService.createGroup(request);
    }

    @DeleteMapping("/internal/group/delete")
    public Map<String, Object> deleteGroup(@RequestBody Map<String, Object> request) {
        return userService.deleteGroup(request);
    }

    @PatchMapping("/internal/group/update")
    public Map<String, Object> updateGroup(@RequestBody Map<String, Object> request) {
        return userService.updateGroup(request);
    }

    @GetMapping("/internal/group/search")
    public Map<String, Object> searchGroups(@RequestParam Map<String, Object> params) {
        return userService.searchGroups(params);
    }

    // ==================== 用户组管理 ====================

    @PostMapping("/internal/group-user/create")
    public Map<String, Object> addUserToGroup(@RequestBody Map<String, Object> request) {
        return userService.addUserToGroup(request);
    }

    @DeleteMapping("/internal/group-user/delete")
    public Map<String, Object> removeUserFromGroup(@RequestBody Map<String, Object> request) {
        return userService.removeUserFromGroup(request);
    }

    @GetMapping("/internal/group-user/search")
    public Map<String, Object> searchGroupUsers(@RequestParam Map<String, Object> params) {
        return userService.searchGroupUsers(params);
    }

    // ==================== 权限管理 ====================

    @PostMapping("/internal/permission/create")
    public Map<String, Object> createPermission(@RequestBody Map<String, Object> request) {
        return userService.createPermission(request);
    }

    @DeleteMapping("/internal/permission/delete")
    public Map<String, Object> deletePermission(@RequestBody Map<String, Object> request) {
        return userService.deletePermission(request);
    }

    @PatchMapping("/internal/permission/update")
    public Map<String, Object> updatePermission(@RequestBody Map<String, Object> request) {
        return userService.updatePermission(request);
    }

    @GetMapping("/internal/permission/search")
    public Map<String, Object> searchPermissions(@RequestParam Map<String, Object> params) {
        return userService.searchPermissions(params);
    }

    // ==================== 分组权限管理 ====================

    @PostMapping("/internal/group-permission/create")
    public Map<String, Object> createGroupPermission(@RequestBody Map<String, Object> request) {
        return userService.createGroupPermission(request);
    }

    @DeleteMapping("/internal/group-permission/delete")
    public Map<String, Object> deleteGroupPermission(@RequestBody Map<String, Object> request) {
        return userService.deleteGroupPermission(request);
    }

    @PatchMapping("/internal/group-permission/update")
    public Map<String, Object> updateGroupPermission(@RequestBody Map<String, Object> request) {
        return userService.updateGroupPermission(request);
    }

    @GetMapping("/internal/group-permission/search")
    public Map<String, Object> searchGroupPermissions(@RequestParam Map<String, Object> params) {
        return userService.searchGroupPermissions(params);
    }
}