package org.example.userservice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.userservice.entity.User;
import org.example.userservice.entity.UserAuth;
import org.example.userservice.mapper.UserAuthMapper;
import org.example.userservice.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户服务：注册、登录、用户管理
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserAuthMapper userAuthMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户注册
     */
    public Map<String, Object> register(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        // 1. 提取参数
        String credentialType = (String) request.get("credentialType");
        String credential = (String) request.get("credential");
        String password = (String) request.get("password");

        // 2. 参数校验
        if (credentialType == null || credential == null || password == null) {
            result.put("code", 400);
            result.put("message", "参数不完整：credentialType, credential, password 必填");
            return result;
        }

        if (!credentialType.equals("PHONE") && !credentialType.equals("EMAIL") && !credentialType.equals("USERNAME")) {
            result.put("code", 400);
            result.put("message", "credentialType 必须为 PHONE / EMAIL / USERNAME");
            return result;
        }

        if (password.length() < 6) {
            result.put("code", 400);
            result.put("message", "密码长度不能少于6位");
            return result;
        }

        // 3. 校验凭证唯一性
        QueryWrapper<UserAuth> authWrapper = new QueryWrapper<>();
        authWrapper.eq("identity_type", credentialType);
        authWrapper.eq("identifier", credential);
        UserAuth existAuth = userAuthMapper.selectOne(authWrapper);
        if (existAuth != null) {
            result.put("code", 409);
            result.put("message", "该凭证已被注册");
            return result;
        }

        // 4. BCrypt 加密密码
        String secretHash = passwordEncoder.encode(password);

        // 5. 创建 user 记录
        User user = new User();
        user.setDisplayName(credential); // 默认显示名为凭证
        user.setStatus("ACTIVE");
        user.setIsDeleted(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        // 6. 创建 user_auth 记录
        UserAuth userAuth = new UserAuth();
        userAuth.setUserId(user.getId());
        userAuth.setIdentityType(credentialType);
        userAuth.setIdentifier(credential);
        userAuth.setSecretHash(secretHash);
        userAuth.setVerified(1);
        userAuth.setCreatedAt(LocalDateTime.now());
        userAuthMapper.insert(userAuth);

        // 7. 返回结果
        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        result.put("data", data);
        return result;
    }

    /**
     * 用户登录
     */
    public Map<String, Object> login(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        // 1. 提取参数
        String credentialType = (String) request.get("credentialType");
        String credential = (String) request.get("credential");
        String password = (String) request.get("password");

        // 2. 参数校验
        if (credentialType == null || credential == null || password == null) {
            result.put("code", 400);
            result.put("message", "参数不完整");
            return result;
        }

        // 3. 查询 user_auth
        QueryWrapper<UserAuth> authWrapper = new QueryWrapper<>();
        authWrapper.eq("identity_type", credentialType);
        authWrapper.eq("identifier", credential);
        UserAuth userAuth = userAuthMapper.selectOne(authWrapper);

        if (userAuth == null) {
            result.put("code", 401);
            result.put("message", "凭证不存在");
            return result;
        }

        // 4. BCrypt 校验密码
        if (!passwordEncoder.matches(password, userAuth.getSecretHash())) {
            result.put("code", 401);
            result.put("message", "密码错误");
            return result;
        }

        // 5. 查询 user 表获取 userId（校验用户状态）
        User user = userMapper.selectById(userAuth.getUserId());
        if (user == null || user.getIsDeleted() == 1) {
            result.put("code", 401);
            result.put("message", "用户不存在或已注销");
            return result;
        }

        // 6. 返回结果
        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("displayName", user.getDisplayName());
        result.put("data", data);
        return result;
    }

    /**
     * 用户注销
     */
    public Map<String, Object> deregister(Long userId) {
        // TODO: 更新 user 表 status 为 DEREGISTERED
        // TODO: 返回 {"code":0, "message":"ok"}
        throw new UnsupportedOperationException("TODO: 实现注销逻辑");
    }

    /**
     * 用户登出
     */
    public Map<String, Object> logout(Long userId) {
        // TODO: 可选：记录登出时间等
        // TODO: 返回 {"code":0, "message":"ok"}
        throw new UnsupportedOperationException("TODO: 实现登出逻辑");
    }

    /**
     * 查询用户权限
     */
    public Map<String, Object> getPermissions(Long userId) {
        Map<String, Object> result = new HashMap<>();
        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            result.put("code", 404);
            result.put("message", "用户不存在");
            return result;
        }

        List<String> roles = new ArrayList<>();
        if (isAdminUser(userId)) {
            roles.add("admin");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("permissions", List.of());
        data.put("roles", roles);
        data.put("is_admin", !roles.isEmpty());

        result.put("code", 0);
        result.put("message", "ok");
        result.put("data", data);
        return result;
    }

    private boolean isAdminUser(Long userId) {
        if (userId != null && userId == 1L) {
            return true;
        }
        QueryWrapper<UserAuth> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("identifier", "admin");
        return userAuthMapper.selectCount(wrapper) > 0;
    }

    /**
     * 查询用户分组
     */
    public Map<String, Object> getGroups(Long userId) {
        // TODO: 联表查询 user_group + group，获取用户所属分组列表
        // TODO: 返回 {"code":0, "data":{"groups":[...]}}
        throw new UnsupportedOperationException("TODO: 实现分组查询");
    }

    // ==================== 管理员接口 ====================

    /**
     * 管理员创建用户
     */
    public Map<String, Object> createUser(Map<String, Object> request) {
        // TODO: 提取参数并校验
        // TODO: BCrypt 加密密码
        // TODO: 创建 user 记录
        // TODO: 创建 user_auth 记录
        // TODO: 可选：分配默认分组
        // TODO: 返回 {"code":0, "data":{"userId":xxx}}
        throw new UnsupportedOperationException("TODO: 实现管理员创建用户");
    }

    /**
     * 管理员删除用户
     */
    public Map<String, Object> deleteUser(Map<String, Object> request) {
        // TODO: 提取 userId
        // TODO: 逻辑删除（设置 is_deleted=1），不物理删除
        // TODO: 返回 {"code":0, "message":"ok"}
        throw new UnsupportedOperationException("TODO: 实现管理员删除用户");
    }

    /**
     * 管理员更新用户
     */
    public Map<String, Object> updateUser(Map<String, Object> request) {
        // TODO: 提取 userId 和要更新的字段
        // TODO: 更新 user 表（display_name, status 等）
        // TODO: 返回 {"code":0, "message":"ok"}
        throw new UnsupportedOperationException("TODO: 实现管理员更新用户");
    }

    /**
     * 管理员查询用户列表
     */
    public Map<String, Object> searchUsers(Map<String, Object> params) {
        // TODO: 支持分页参数：page, size
        // TODO: 支持筛选条件：status, keyword（按 display_name 模糊查询）
        // TODO: 返回 {"code":0, "data":{"list":[...], "total":100}}
        throw new UnsupportedOperationException("TODO: 实现用户搜索");
    }

    // ==================== 分组管理 ====================

    public Map<String, Object> createGroup(Map<String, Object> request) {
        // TODO: 提取 name, description, creatorUserId
        // TODO: 创建 group 记录
        // TODO: 返回 {"code":0, "data":{"groupId":xxx}}
        throw new UnsupportedOperationException("TODO: 实现创建分组");
    }

    public Map<String, Object> deleteGroup(Map<String, Object> request) {
        // TODO: 提取 groupId，逻辑删除
        throw new UnsupportedOperationException("TODO: 实现删除分组");
    }

    public Map<String, Object> updateGroup(Map<String, Object> request) {
        // TODO: 提取 groupId 和要更新的字段
        throw new UnsupportedOperationException("TODO: 实现更新分组");
    }

    public Map<String, Object> searchGroups(Map<String, Object> params) {
        // TODO: 支持分页和关键字筛选
        throw new UnsupportedOperationException("TODO: 实现分组查询");
    }

    // ==================== 用户组管理 ====================

    public Map<String, Object> addUserToGroup(Map<String, Object> request) {
        // TODO: 提取 userId, groupId
        // TODO: 校验用户和分组是否存在
        // TODO: 校验是否已在组中（避免重复添加）
        // TODO: 创建 user_group 记录
        throw new UnsupportedOperationException("TODO: 实现添加用户到组");
    }

    public Map<String, Object> removeUserFromGroup(Map<String, Object> request) {
        // TODO: 提取 userId, groupId
        // TODO: 删除 user_group 记录
        throw new UnsupportedOperationException("TODO: 实现从组移除用户");
    }

    public Map<String, Object> searchGroupUsers(Map<String, Object> params) {
        // TODO: 提取 groupId，查询该组所有用户
        // TODO: 支持分页
        throw new UnsupportedOperationException("TODO: 实现查询组成员");
    }

    // ==================== 权限管理 ====================

    public Map<String, Object> createPermission(Map<String, Object> request) {
        // TODO: 提取 permCode, permName, description
        // TODO: 创建 permission 记录
        throw new UnsupportedOperationException("TODO: 实现创建权限");
    }

    public Map<String, Object> deletePermission(Map<String, Object> request) {
        // TODO: 提取 permissionId，逻辑删除
        throw new UnsupportedOperationException("TODO: 实现删除权限");
    }

    public Map<String, Object> updatePermission(Map<String, Object> request) {
        // TODO: 提取 permissionId 和要更新的字段
        throw new UnsupportedOperationException("TODO: 实现更新权限");
    }

    public Map<String, Object> searchPermissions(Map<String, Object> params) {
        // TODO: 支持分页和关键字筛选
        throw new UnsupportedOperationException("TODO: 实现权限查询");
    }

    // ==================== 分组权限管理 ====================

    public Map<String, Object> createGroupPermission(Map<String, Object> request) {
        // TODO: 提取 groupId, permissionId
        // TODO: 创建 group_permission 记录
        throw new UnsupportedOperationException("TODO: 实现创建分组权限");
    }

    public Map<String, Object> deleteGroupPermission(Map<String, Object> request) {
        // TODO: 提取 groupPermissionId，删除记录
        throw new UnsupportedOperationException("TODO: 实现删除分组权限");
    }

    public Map<String, Object> updateGroupPermission(Map<String, Object> request) {
        // TODO: 提取 groupPermissionId 和要更新的字段
        throw new UnsupportedOperationException("TODO: 实现更新分组权限");
    }

    public Map<String, Object> searchGroupPermissions(Map<String, Object> params) {
        // TODO: 提取 groupId，查询该组所有权限
        throw new UnsupportedOperationException("TODO: 实现分组权限查询");
    }
}