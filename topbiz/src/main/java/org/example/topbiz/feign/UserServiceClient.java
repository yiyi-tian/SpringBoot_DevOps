package org.example.topbiz.feign;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.*;

import java.util.Map;

@HttpExchange("/internal")
public interface UserServiceClient {

    @PostExchange("/user/register")
    Map<String, Object> register(@RequestBody Map<String, Object> request);

    @PostExchange("/user/register/compensate")
    Map<String, Object> compensateRegister(@RequestBody Map<String, Object> request);

    @PostExchange("/user/login")
    Map<String, Object> login(@RequestBody Map<String, Object> request);

    @PostExchange("/user/{userId}/deregister")
    Map<String, Object> deregister(@PathVariable Long userId);

    @PostExchange("/user/{userId}/logout")
    Map<String, Object> logout(@PathVariable Long userId);

    @GetExchange("/user/{userId}/permissions")
    Map<String, Object> getPermissions(@PathVariable Long userId);

    @GetExchange("/user/{userId}/groups")
    Map<String, Object> getGroups(@PathVariable Long userId);

    // 用户自助服务
    @PostExchange("/user/password")
    Map<String, Object> changePassword(@RequestBody Map<String, Object> request);

    @PatchExchange("/user/profile")
    Map<String, Object> updateProfile(@RequestBody Map<String, Object> request);

    @GetExchange("/user/{userId}/profile")
    Map<String, Object> getUserProfile(@PathVariable Long userId);

    @PostExchange("/user/password/reset")
    Map<String, Object> resetPassword(@RequestBody Map<String, Object> request);

    @PostExchange("/user/bind")
    Map<String, Object> bindCredential(@RequestBody Map<String, Object> request);

    // 管理员接口
    @PostExchange("/user/create")
    Map<String, Object> createUser(@RequestBody Map<String, Object> request);

    @DeleteExchange("/user/delete")
    Map<String, Object> deleteUser(@RequestBody Map<String, Object> request);

    @PatchExchange("/user/update")
    Map<String, Object> updateUser(@RequestBody Map<String, Object> request);

    @GetExchange("/user/search")
    Map<String, Object> searchUsers(@RequestParam Map<String, Object> params);

    @PostExchange("/group/create")
    Map<String, Object> createGroup(@RequestBody Map<String, Object> request);

    @DeleteExchange("/group/delete")
    Map<String, Object> deleteGroup(@RequestBody Map<String, Object> request);

    @PatchExchange("/group/update")
    Map<String, Object> updateGroup(@RequestBody Map<String, Object> request);

    @GetExchange("/group/search")
    Map<String, Object> searchGroups(@RequestParam Map<String, Object> params);

    @PostExchange("/group-user/create")
    Map<String, Object> addUserToGroup(@RequestBody Map<String, Object> request);

    @DeleteExchange("/group-user/delete")
    Map<String, Object> removeUserFromGroup(@RequestBody Map<String, Object> request);

    @GetExchange("/group-user/search")
    Map<String, Object> searchGroupUsers(@RequestParam Map<String, Object> params);

    @PostExchange("/permission/create")
    Map<String, Object> createPermission(@RequestBody Map<String, Object> request);

    @DeleteExchange("/permission/delete")
    Map<String, Object> deletePermission(@RequestBody Map<String, Object> request);

    @PatchExchange("/permission/update")
    Map<String, Object> updatePermission(@RequestBody Map<String, Object> request);

    @GetExchange("/permission/search")
    Map<String, Object> searchPermissions(@RequestParam Map<String, Object> params);

    @PostExchange("/group-permission/create")
    Map<String, Object> createGroupPermission(@RequestBody Map<String, Object> request);

    @DeleteExchange("/group-permission/delete")
    Map<String, Object> deleteGroupPermission(@RequestBody Map<String, Object> request);

    @PatchExchange("/group-permission/update")
    Map<String, Object> updateGroupPermission(@RequestBody Map<String, Object> request);

    @GetExchange("/group-permission/search")
    Map<String, Object> searchGroupPermissions(@RequestParam Map<String, Object> params);

    // 用户直接权限管理
    @PostExchange("/user-permission/create")
    Map<String, Object> createUserPermission(@RequestBody Map<String, Object> request);

    @DeleteExchange("/user-permission/delete")
    Map<String, Object> deleteUserPermission(@RequestBody Map<String, Object> request);

    @PatchExchange("/user-permission/update")
    Map<String, Object> updateUserPermission(@RequestBody Map<String, Object> request);

    @GetExchange("/user-permission/search")
    Map<String, Object> searchUserPermissions(@RequestParam Map<String, Object> params);

    // 权限申请与审批
    @PostExchange("/user-permission/apply")
    Map<String, Object> applyUserPermission(@RequestBody Map<String, Object> request);

    @PostExchange("/user-permission/approve")
    Map<String, Object> approveUserPermission(@RequestBody Map<String, Object> request);

    @PostExchange("/user-permission/reject")
    Map<String, Object> rejectUserPermission(@RequestBody Map<String, Object> request);

    @PostExchange("/group-permission/apply")
    Map<String, Object> applyGroupPermission(@RequestBody Map<String, Object> request);

    @PostExchange("/group-permission/approve")
    Map<String, Object> approveGroupPermission(@RequestBody Map<String, Object> request);

    @PostExchange("/group-permission/reject")
    Map<String, Object> rejectGroupPermission(@RequestBody Map<String, Object> request);

    // 自驱任务
    @PostExchange("/scheduler/expire-stale-auths")
    Map<String, Object> expireStaleAuths(@RequestBody Map<String, Object> request);

    @PostExchange("/scheduler/deactivate-inactive-users")
    Map<String, Object> deactivateInactiveUsers(@RequestBody Map<String, Object> request);

    @PostExchange("/scheduler/purge-deregistered-users")
    Map<String, Object> purgeDeregisteredUsers(@RequestBody Map<String, Object> request);

    @PostExchange("/scheduler/clean-expired-caches")
    Map<String, Object> cleanExpiredCaches();

    @PostExchange("/scheduler/clean-expired-data")
    Map<String, Object> cleanExpiredData(@RequestBody Map<String, Object> request);

    // v0.4.0: IP 突变检测 & 多端会话管理
    @PostExchange("/user/login-history/scan")
    Map<String, Object> scanIpMutations(@RequestBody Map<String, Object> request);

    @PostExchange("/user/session")
    Map<String, Object> registerSession(@RequestBody Map<String, Object> request);

    @GetExchange("/user/{userId}/sessions")
    Map<String, Object> getUserSessions(@PathVariable Long userId);

    @DeleteExchange("/user/sessions/{sessionId}")
    Map<String, Object> terminateSession(@PathVariable String sessionId);

    @DeleteExchange("/user/{userId}/devices/{deviceId}")
    Map<String, Object> terminateDevice(@PathVariable Long userId, @PathVariable String deviceId);

    @DeleteExchange("/user/{userId}/sessions/others")
    Map<String, Object> terminateOtherSessions(@PathVariable Long userId, @RequestBody Map<String, Object> request);

    @PostExchange("/user/sessions/clean-stale")
    Map<String, Object> cleanStaleSessions(@RequestBody Map<String, Object> request);
}