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

    @PostMapping("/internal/user/register/compensate")
    public Map<String, Object> compensateRegister(@RequestBody Map<String, Object> request) {
        return userService.compensateRegister(request);
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

    // ==================== 用户自助服务 ====================

    @PostMapping("/internal/user/password")
    public Map<String, Object> changePassword(@RequestBody Map<String, Object> request) {
        return userService.changePassword(request);
    }

    @PatchMapping("/internal/user/profile")
    public Map<String, Object> updateProfile(@RequestBody Map<String, Object> request) {
        return userService.updateProfile(request);
    }

    @GetMapping("/internal/user/{userId}/profile")
    public Map<String, Object> getUserProfile(@PathVariable Long userId) {
        return userService.getUserProfile(userId);
    }

    @PostMapping("/internal/user/password/reset")
    public Map<String, Object> resetPassword(@RequestBody Map<String, Object> request) {
        return userService.resetPassword(request);
    }

    @PostMapping("/internal/user/bind")
    public Map<String, Object> bindCredential(@RequestBody Map<String, Object> request) {
        return userService.bindCredential(request);
    }

    // ==================== 用户直接权限管理 ====================

    @PostMapping("/internal/user-permission/create")
    public Map<String, Object> createUserPermission(@RequestBody Map<String, Object> request) {
        return userService.createUserPermission(request);
    }

    @DeleteMapping("/internal/user-permission/delete")
    public Map<String, Object> deleteUserPermission(@RequestBody Map<String, Object> request) {
        return userService.deleteUserPermission(request);
    }

    @PatchMapping("/internal/user-permission/update")
    public Map<String, Object> updateUserPermission(@RequestBody Map<String, Object> request) {
        return userService.updateUserPermission(request);
    }

    @GetMapping("/internal/user-permission/search")
    public Map<String, Object> searchUserPermissions(@RequestParam Map<String, Object> params) {
        return userService.searchUserPermissions(params);
    }

    // ==================== 权限申请与审批 ====================

    @PostMapping("/internal/user-permission/apply")
    public Map<String, Object> applyUserPermission(@RequestBody Map<String, Object> request) {
        return userService.applyUserPermission(request);
    }

    @PostMapping("/internal/user-permission/approve")
    public Map<String, Object> approveUserPermission(@RequestBody Map<String, Object> request) {
        return userService.approveUserPermission(request);
    }

    @PostMapping("/internal/user-permission/reject")
    public Map<String, Object> rejectUserPermission(@RequestBody Map<String, Object> request) {
        return userService.rejectUserPermission(request);
    }

    @PostMapping("/internal/group-permission/apply")
    public Map<String, Object> applyGroupPermission(@RequestBody Map<String, Object> request) {
        return userService.applyGroupPermission(request);
    }

    @PostMapping("/internal/group-permission/approve")
    public Map<String, Object> approveGroupPermission(@RequestBody Map<String, Object> request) {
        return userService.approveGroupPermission(request);
    }

    @PostMapping("/internal/group-permission/reject")
    public Map<String, Object> rejectGroupPermission(@RequestBody Map<String, Object> request) {
        return userService.rejectGroupPermission(request);
    }

    // ==================== 自驱任务 ====================

    @PostMapping("/internal/scheduler/expire-stale-auths")
    public Map<String, Object> expireStaleAuths(@RequestBody Map<String, Object> request) {
        int days = request.containsKey("days") ? Integer.parseInt(String.valueOf(request.get("days"))) : 14;
        return userService.expireStaleAuths(days);
    }

    @PostMapping("/internal/scheduler/deactivate-inactive-users")
    public Map<String, Object> deactivateInactiveUsers(@RequestBody Map<String, Object> request) {
        int days = request.containsKey("days") ? Integer.parseInt(String.valueOf(request.get("days"))) : 30;
        return userService.deactivateInactiveUsers(days);
    }

    @PostMapping("/internal/scheduler/purge-deregistered-users")
    public Map<String, Object> purgeDeregisteredUsers(@RequestBody Map<String, Object> request) {
        int days = request.containsKey("days") ? Integer.parseInt(String.valueOf(request.get("days"))) : 30;
        return userService.purgeDeregisteredUsers(days);
    }

    @PostMapping("/internal/scheduler/clean-expired-caches")
    public Map<String, Object> cleanExpiredCaches() {
        return userService.cleanExpiredCaches();
    }

    @PostMapping("/internal/scheduler/clean-expired-data")
    public Map<String, Object> cleanExpiredData(@RequestBody Map<String, Object> request) {
        int days = request.containsKey("days") ? Integer.parseInt(String.valueOf(request.get("days"))) : 90;
        return userService.cleanExpiredData(days);
    }

    // ==================== v0.4.0: IP 突变检测 & 多端会话管理 ====================

    @PostMapping("/internal/user/login-history/scan")
    public Map<String, Object> scanIpMutations(@RequestBody Map<String, Object> request) {
        return userService.scanIpMutations(request);
    }

    @PostMapping("/internal/user/session")
    public Map<String, Object> registerSession(@RequestBody Map<String, Object> request) {
        return userService.registerSession(request);
    }

    @GetMapping("/internal/user/{userId}/sessions")
    public Map<String, Object> getUserSessions(@PathVariable Long userId) {
        return userService.getUserSessions(userId);
    }

    @DeleteMapping("/internal/user/sessions/{sessionId}")
    public Map<String, Object> terminateSession(@PathVariable String sessionId) {
        return userService.terminateSession(sessionId);
    }

    @DeleteMapping("/internal/user/{userId}/devices/{deviceId}")
    public Map<String, Object> terminateDevice(@PathVariable Long userId, @PathVariable String deviceId) {
        return userService.terminateDevice(userId, deviceId);
    }

    @DeleteMapping("/internal/user/{userId}/sessions/others")
    public Map<String, Object> terminateOtherSessions(@PathVariable Long userId, @RequestBody Map<String, Object> request) {
        String currentSessionId = (String) request.get("currentSessionId");
        String currentDeviceId = (String) request.get("currentDeviceId");
        return userService.terminateOtherSessions(userId, currentSessionId, currentDeviceId);
    }

    @PostMapping("/internal/user/sessions/clean-stale")
    public Map<String, Object> cleanStaleSessions(@RequestBody Map<String, Object> request) {
        return userService.cleanStaleSessions(request);
    }
}