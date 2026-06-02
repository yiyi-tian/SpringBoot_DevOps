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

    @PostExchange("/user/login")
    Map<String, Object> login(@RequestBody Map<String, Object> request);

    @DeleteExchange("/user/{userId}/deregister")
    Map<String, Object> deregister(@PathVariable Long userId);

    @PatchExchange("/user/{userId}/logout")
    Map<String, Object> logout(@PathVariable Long userId);

    @GetExchange("/user/{userId}/permissions")
    Map<String, Object> getPermissions(@PathVariable Long userId);

    @GetExchange("/user/{userId}/groups")
    Map<String, Object> getGroups(@PathVariable Long userId);

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
}