package org.example.topbiz.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@FeignClient(name = "user-service", url = "http://localhost:8081")
public interface UserServiceClient {

    @PostMapping("/internal/user/register")
    Map<String, Object> register(@RequestBody Map<String, Object> request);

    @PostMapping("/internal/user/login")
    Map<String, Object> login(@RequestBody Map<String, Object> request);

    @PostMapping("/internal/user/{userId}/deregister")
    Map<String, Object> deregister(@PathVariable Long userId);

    @PostMapping("/internal/user/{userId}/logout")
    Map<String, Object> logout(@PathVariable Long userId);

    @GetMapping("/internal/user/{userId}/permissions")
    Map<String, Object> getPermissions(@PathVariable Long userId);

    @GetMapping("/internal/user/{userId}/groups")
    Map<String, Object> getGroups(@PathVariable Long userId);

    // 管理员接口
    @PostMapping("/internal/user/create")
    Map<String, Object> createUser(@RequestBody Map<String, Object> request);

    @DeleteMapping("/internal/user/delete")
    Map<String, Object> deleteUser(@RequestBody Map<String, Object> request);

    @PatchMapping("/internal/user/update")
    Map<String, Object> updateUser(@RequestBody Map<String, Object> request);

    @GetMapping("/internal/user/search")
    Map<String, Object> searchUsers(@RequestParam Map<String, Object> params);

    @PostMapping("/internal/group/create")
    Map<String, Object> createGroup(@RequestBody Map<String, Object> request);

    @DeleteMapping("/internal/group/delete")
    Map<String, Object> deleteGroup(@RequestBody Map<String, Object> request);

    @PatchMapping("/internal/group/update")
    Map<String, Object> updateGroup(@RequestBody Map<String, Object> request);

    @GetMapping("/internal/group/search")
    Map<String, Object> searchGroups(@RequestParam Map<String, Object> params);

    @PostMapping("/internal/group-user/create")
    Map<String, Object> addUserToGroup(@RequestBody Map<String, Object> request);

    @DeleteMapping("/internal/group-user/delete")
    Map<String, Object> removeUserFromGroup(@RequestBody Map<String, Object> request);

    @GetMapping("/internal/group-user/search")
    Map<String, Object> searchGroupUsers(@RequestParam Map<String, Object> params);

    @PostMapping("/internal/permission/create")
    Map<String, Object> createPermission(@RequestBody Map<String, Object> request);

    @DeleteMapping("/internal/permission/delete")
    Map<String, Object> deletePermission(@RequestBody Map<String, Object> request);

    @PatchMapping("/internal/permission/update")
    Map<String, Object> updatePermission(@RequestBody Map<String, Object> request);

    @GetMapping("/internal/permission/search")
    Map<String, Object> searchPermissions(@RequestParam Map<String, Object> params);

    @PostMapping("/internal/group-permission/create")
    Map<String, Object> createGroupPermission(@RequestBody Map<String, Object> request);

    @DeleteMapping("/internal/group-permission/delete")
    Map<String, Object> deleteGroupPermission(@RequestBody Map<String, Object> request);

    @PatchMapping("/internal/group-permission/update")
    Map<String, Object> updateGroupPermission(@RequestBody Map<String, Object> request);

    @GetMapping("/internal/group-permission/search")
    Map<String, Object> searchGroupPermissions(@RequestParam Map<String, Object> params);
}