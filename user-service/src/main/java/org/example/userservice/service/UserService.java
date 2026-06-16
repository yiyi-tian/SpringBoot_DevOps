package org.example.userservice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.userservice.entity.*;
import org.example.userservice.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 用户服务：注册、登录、用户管理、分组管理、权限管理
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserAuthMapper userAuthMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private UserGroupMapper userGroupMapper;

    @Autowired
    private GroupPermissionMapper groupPermissionMapper;

    @Autowired
    private UserPermissionMapper userPermissionMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ==================== 注册 & 登录 ====================

    /**
     * 用户注册
     */
    public Map<String, Object> register(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        String credentialType = (String) request.get("credentialType");
        String credential = (String) request.get("credential");
        String password = (String) request.get("password");

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

        QueryWrapper<UserAuth> authWrapper = new QueryWrapper<>();
        authWrapper.eq("identity_type", credentialType);
        authWrapper.eq("identifier", credential);
        if (userAuthMapper.selectOne(authWrapper) != null) {
            result.put("code", 409);
            result.put("message", "该凭证已被注册");
            return result;
        }

        String secretHash = passwordEncoder.encode(password);

        User user = new User();
        user.setDisplayName(credential);
        user.setStatus("ACTIVE");
        user.setIsDeleted(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        UserAuth userAuth = new UserAuth();
        userAuth.setUserId(user.getUserId());
        userAuth.setIdentityType(credentialType);
        userAuth.setIdentifier(credential);
        userAuth.setSecretHash(secretHash);
        userAuth.setVerified(1);
        userAuth.setCreatedAt(LocalDateTime.now());
        userAuthMapper.insert(userAuth);

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        result.put("data", data);
        return result;
    }

    /**
     * 用户登录
     */
    public Map<String, Object> login(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        String credentialType = (String) request.get("credentialType");
        String credential = (String) request.get("credential");
        String password = (String) request.get("password");

        if (credentialType == null || credential == null || password == null) {
            result.put("code", 400);
            result.put("message", "参数不完整");
            return result;
        }

        QueryWrapper<UserAuth> authWrapper = new QueryWrapper<>();
        authWrapper.eq("identity_type", credentialType);
        authWrapper.eq("identifier", credential);
        UserAuth userAuth = userAuthMapper.selectOne(authWrapper);

        if (userAuth == null) {
            result.put("code", 401);
            result.put("message", "凭证不存在");
            return result;
        }

        if (!passwordEncoder.matches(password, userAuth.getSecretHash())) {
            result.put("code", 401);
            result.put("message", "密码错误");
            return result;
        }

        User user = userMapper.selectById(userAuth.getUserId());
        if (user == null || user.getIsDeleted() == 1) {
            result.put("code", 401);
            result.put("message", "用户不存在或已注销");
            return result;
        }

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("displayName", user.getDisplayName());
        result.put("data", data);
        return result;
    }

    // ==================== 用户生命周期 ====================

    /**
     * 用户注销：更新状态为 DEREGISTERED
     */
    public Map<String, Object> deregister(Long userId) {
        Map<String, Object> result = new HashMap<>();

        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            result.put("code", 404);
            result.put("message", "用户不存在");
            return result;
        }

        user.setStatus("DEREGISTERED");
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        result.put("code", 0);
        result.put("message", "ok");
        return result;
    }

    /**
     * 用户登出（会话由 topbiz Shiro 管理，此处仅返回成功）
     */
    public Map<String, Object> logout(Long userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "ok");
        return result;
    }

    /**
     * 查询用户权限：user_permission（直接）∪ group_permission（通过分组）
     */
    public Map<String, Object> getPermissions(Long userId) {
        Map<String, Object> result = new HashMap<>();
        Set<String> permCodes = new LinkedHashSet<>();

        // 1. 直接权限
        QueryWrapper<UserPermission> upWrapper = new QueryWrapper<>();
        upWrapper.eq("user_id", userId);
        for (UserPermission up : userPermissionMapper.selectList(upWrapper)) {
            Permission p = permissionMapper.selectById(up.getPermId());
            if (p != null && p.getActive() == 1) {
                permCodes.add(p.getPermCode());
            }
        }

        // 2. 通过分组的权限
        QueryWrapper<UserGroup> ugWrapper = new QueryWrapper<>();
        ugWrapper.eq("user_id", userId);
        for (UserGroup ug : userGroupMapper.selectList(ugWrapper)) {
            QueryWrapper<GroupPermission> gpWrapper = new QueryWrapper<>();
            gpWrapper.eq("group_id", ug.getGroupId());
            for (GroupPermission gp : groupPermissionMapper.selectList(gpWrapper)) {
                Permission p = permissionMapper.selectById(gp.getPermId());
                if (p != null && p.getActive() == 1) {
                    permCodes.add(p.getPermCode());
                }
            }
        }

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("permissions", new ArrayList<>(permCodes));
        result.put("data", data);
        return result;
    }

    /**
     * 查询用户所属分组
     */
    public Map<String, Object> getGroups(Long userId) {
        Map<String, Object> result = new HashMap<>();

        QueryWrapper<UserGroup> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        List<UserGroup> userGroups = userGroupMapper.selectList(wrapper);

        List<Map<String, Object>> groups = new ArrayList<>();
        for (UserGroup ug : userGroups) {
            Group group = groupMapper.selectById(ug.getGroupId());
            if (group != null && group.getIsDeleted() == 0) {
                Map<String, Object> g = new HashMap<>();
                g.put("groupId", group.getGroupId());
                g.put("name", group.getName());
                g.put("description", group.getDescription());
                groups.add(g);
            }
        }

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("groups", groups);
        result.put("data", data);
        return result;
    }

    // ==================== 管理员：用户管理 ====================

    /**
     * 管理员创建用户
     */
    public Map<String, Object> createUser(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        String credentialType = (String) request.get("credentialType");
        String credential = (String) request.get("credential");
        String password = (String) request.get("password");
        String displayName = (String) request.getOrDefault("displayName", credential);

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

        QueryWrapper<UserAuth> authWrapper = new QueryWrapper<>();
        authWrapper.eq("identity_type", credentialType);
        authWrapper.eq("identifier", credential);
        if (userAuthMapper.selectOne(authWrapper) != null) {
            result.put("code", 409);
            result.put("message", "该凭证已被使用");
            return result;
        }

        String secretHash = passwordEncoder.encode(password);

        User user = new User();
        user.setDisplayName(displayName);
        user.setStatus("ACTIVE");
        user.setIsDeleted(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        UserAuth userAuth = new UserAuth();
        userAuth.setUserId(user.getUserId());
        userAuth.setIdentityType(credentialType);
        userAuth.setIdentifier(credential);
        userAuth.setSecretHash(secretHash);
        userAuth.setVerified(1);
        userAuth.setCreatedAt(LocalDateTime.now());
        userAuthMapper.insert(userAuth);

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        result.put("data", data);
        return result;
    }

    /**
     * 管理员删除用户（逻辑删除）
     */
    public Map<String, Object> deleteUser(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        Object userIdObj = request.get("userId");
        if (userIdObj == null) {
            result.put("code", 400);
            result.put("message", "userId 必填");
            return result;
        }
        Long userId = Long.valueOf(String.valueOf(userIdObj));

        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            result.put("code", 404);
            result.put("message", "用户不存在");
            return result;
        }

        user.setIsDeleted(1);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        result.put("code", 0);
        result.put("message", "ok");
        return result;
    }

    /**
     * 管理员更新用户
     */
    public Map<String, Object> updateUser(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        Object userIdObj = request.get("userId");
        if (userIdObj == null) {
            result.put("code", 400);
            result.put("message", "userId 必填");
            return result;
        }
        Long userId = Long.valueOf(String.valueOf(userIdObj));

        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            result.put("code", 404);
            result.put("message", "用户不存在");
            return result;
        }

        if (request.containsKey("displayName")) {
            user.setDisplayName((String) request.get("displayName"));
        }
        if (request.containsKey("status")) {
            user.setStatus((String) request.get("status"));
        }
        if (request.containsKey("sex")) {
            Object sexObj = request.get("sex");
            user.setSex(sexObj != null ? Integer.valueOf(String.valueOf(sexObj)) : null);
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        result.put("code", 0);
        result.put("message", "ok");
        return result;
    }

    /**
     * 管理员查询用户列表（分页 + 条件筛选）
     */
    public Map<String, Object> searchUsers(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();

        int page = parseIntOrDefault(params.get("page"), 1);
        int size = parseIntOrDefault(params.get("size"), 10);
        String keyword = (String) params.get("keyword");
        String status = (String) params.get("status");

        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0);
        if (status != null && !status.isEmpty()) {
            wrapper.eq("status", status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like("display_name", keyword);
        }
        wrapper.orderByDesc("created_at");

        long total = userMapper.selectCount(wrapper);
        int offset = (page - 1) * size;
        wrapper.last("LIMIT " + offset + "," + size);
        List<User> users = userMapper.selectList(wrapper);

        List<Map<String, Object>> list = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> item = new HashMap<>();
            item.put("userId", u.getUserId());
            item.put("displayName", u.getDisplayName());
            item.put("sex", u.getSex());
            item.put("status", u.getStatus());
            item.put("createdAt", u.getCreatedAt());
            list.add(item);
        }

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        result.put("data", data);
        return result;
    }

    // ==================== 分组管理 ====================

    public Map<String, Object> createGroup(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        String name = (String) request.get("name");
        if (name == null || name.isEmpty()) {
            result.put("code", 400);
            result.put("message", "name 必填");
            return result;
        }

        Group group = new Group();
        group.setName(name);
        group.setDescription((String) request.get("description"));

        Object creatorObj = request.get("creatorUserId");
        if (creatorObj != null) {
            group.setCreatorUserId(Long.valueOf(String.valueOf(creatorObj)));
        }

        group.setIsAdmin(0);
        group.setIsDeleted(0);
        group.setCreatedAt(LocalDateTime.now());
        groupMapper.insert(group);

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("groupId", group.getGroupId());
        result.put("data", data);
        return result;
    }

    public Map<String, Object> deleteGroup(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        Object groupIdObj = request.get("groupId");
        if (groupIdObj == null) {
            result.put("code", 400);
            result.put("message", "groupId 必填");
            return result;
        }
        Long groupId = Long.valueOf(String.valueOf(groupIdObj));

        Group group = groupMapper.selectById(groupId);
        if (group == null || group.getIsDeleted() == 1) {
            result.put("code", 404);
            result.put("message", "分组不存在");
            return result;
        }

        group.setIsDeleted(1);
        groupMapper.updateById(group);

        result.put("code", 0);
        result.put("message", "ok");
        return result;
    }

    public Map<String, Object> updateGroup(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        Object groupIdObj = request.get("groupId");
        if (groupIdObj == null) {
            result.put("code", 400);
            result.put("message", "groupId 必填");
            return result;
        }
        Long groupId = Long.valueOf(String.valueOf(groupIdObj));

        Group group = groupMapper.selectById(groupId);
        if (group == null || group.getIsDeleted() == 1) {
            result.put("code", 404);
            result.put("message", "分组不存在");
            return result;
        }

        if (request.containsKey("name")) {
            group.setName((String) request.get("name"));
        }
        if (request.containsKey("description")) {
            group.setDescription((String) request.get("description"));
        }
        groupMapper.updateById(group);

        result.put("code", 0);
        result.put("message", "ok");
        return result;
    }

    public Map<String, Object> searchGroups(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();

        int page = parseIntOrDefault(params.get("page"), 1);
        int size = parseIntOrDefault(params.get("size"), 10);
        String keyword = (String) params.get("keyword");

        QueryWrapper<Group> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like("name", keyword);
        }
        wrapper.orderByDesc("created_at");

        long total = groupMapper.selectCount(wrapper);
        int offset = (page - 1) * size;
        wrapper.last("LIMIT " + offset + "," + size);
        List<Group> groups = groupMapper.selectList(wrapper);

        List<Map<String, Object>> list = new ArrayList<>();
        for (Group g : groups) {
            Map<String, Object> item = new HashMap<>();
            item.put("groupId", g.getGroupId());
            item.put("name", g.getName());
            item.put("description", g.getDescription());
            item.put("creatorUserId", g.getCreatorUserId());
            item.put("isAdmin", g.getIsAdmin());
            item.put("createdAt", g.getCreatedAt());
            list.add(item);
        }

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        result.put("data", data);
        return result;
    }

    // ==================== 用户组管理 ====================

    public Map<String, Object> addUserToGroup(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        Object userIdObj = request.get("userId");
        Object groupIdObj = request.get("groupId");
        if (userIdObj == null || groupIdObj == null) {
            result.put("code", 400);
            result.put("message", "userId 和 groupId 必填");
            return result;
        }
        Long userId = Long.valueOf(String.valueOf(userIdObj));
        Long groupId = Long.valueOf(String.valueOf(groupIdObj));

        // 校验用户存在
        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            result.put("code", 404);
            result.put("message", "用户不存在");
            return result;
        }

        // 校验分组存在
        Group group = groupMapper.selectById(groupId);
        if (group == null || group.getIsDeleted() == 1) {
            result.put("code", 404);
            result.put("message", "分组不存在");
            return result;
        }

        // 检查是否已在组中
        QueryWrapper<UserGroup> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("group_id", groupId);
        if (userGroupMapper.selectOne(wrapper) != null) {
            result.put("code", 409);
            result.put("message", "用户已在该分组中");
            return result;
        }

        UserGroup userGroup = new UserGroup();
        userGroup.setUserId(userId);
        userGroup.setGroupId(groupId);
        userGroupMapper.insert(userGroup);

        result.put("code", 0);
        result.put("message", "ok");
        return result;
    }

    public Map<String, Object> removeUserFromGroup(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        Object userIdObj = request.get("userId");
        Object groupIdObj = request.get("groupId");
        if (userIdObj == null || groupIdObj == null) {
            result.put("code", 400);
            result.put("message", "userId 和 groupId 必填");
            return result;
        }
        Long userId = Long.valueOf(String.valueOf(userIdObj));
        Long groupId = Long.valueOf(String.valueOf(groupIdObj));

        QueryWrapper<UserGroup> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("group_id", groupId);
        UserGroup userGroup = userGroupMapper.selectOne(wrapper);

        if (userGroup == null) {
            result.put("code", 404);
            result.put("message", "用户不在该分组中");
            return result;
        }

        userGroupMapper.deleteById(userGroup.getId());

        result.put("code", 0);
        result.put("message", "ok");
        return result;
    }

    public Map<String, Object> searchGroupUsers(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();

        Object groupIdObj = params.get("groupId");
        if (groupIdObj == null) {
            result.put("code", 400);
            result.put("message", "groupId 必填");
            return result;
        }
        Long groupId = Long.valueOf(String.valueOf(groupIdObj));

        int page = parseIntOrDefault(params.get("page"), 1);
        int size = parseIntOrDefault(params.get("size"), 10);

        QueryWrapper<UserGroup> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id", groupId);

        long total = userGroupMapper.selectCount(wrapper);
        int offset = (page - 1) * size;
        wrapper.last("LIMIT " + offset + "," + size);
        List<UserGroup> userGroups = userGroupMapper.selectList(wrapper);

        List<Map<String, Object>> list = new ArrayList<>();
        for (UserGroup ug : userGroups) {
            User user = userMapper.selectById(ug.getUserId());
            if (user != null && user.getIsDeleted() == 0) {
                Map<String, Object> item = new HashMap<>();
                item.put("userId", user.getUserId());
                item.put("displayName", user.getDisplayName());
                item.put("status", user.getStatus());
                list.add(item);
            }
        }

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        result.put("data", data);
        return result;
    }

    // ==================== 权限管理 ====================

    public Map<String, Object> createPermission(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        String permCode = (String) request.get("permCode");
        String permName = (String) request.get("permName");
        if (permCode == null || permCode.isEmpty() || permName == null || permName.isEmpty()) {
            result.put("code", 400);
            result.put("message", "permCode 和 permName 必填");
            return result;
        }

        // 检查编码唯一性
        QueryWrapper<Permission> wrapper = new QueryWrapper<>();
        wrapper.eq("perm_code", permCode);
        if (permissionMapper.selectOne(wrapper) != null) {
            result.put("code", 409);
            result.put("message", "权限编码已存在");
            return result;
        }

        Permission permission = new Permission();
        permission.setPermCode(permCode);
        permission.setPermName(permName);
        permission.setActive(1);
        permissionMapper.insert(permission);

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("permId", permission.getPermId());
        result.put("data", data);
        return result;
    }

    public Map<String, Object> deletePermission(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        Object permIdObj = request.get("permId");
        if (permIdObj == null) {
            result.put("code", 400);
            result.put("message", "permId 必填");
            return result;
        }
        Long permId = Long.valueOf(String.valueOf(permIdObj));

        Permission permission = permissionMapper.selectById(permId);
        if (permission == null) {
            result.put("code", 404);
            result.put("message", "权限不存在");
            return result;
        }

        permission.setActive(0);
        permissionMapper.updateById(permission);

        result.put("code", 0);
        result.put("message", "ok");
        return result;
    }

    public Map<String, Object> updatePermission(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        Object permIdObj = request.get("permId");
        if (permIdObj == null) {
            result.put("code", 400);
            result.put("message", "permId 必填");
            return result;
        }
        Long permId = Long.valueOf(String.valueOf(permIdObj));

        Permission permission = permissionMapper.selectById(permId);
        if (permission == null) {
            result.put("code", 404);
            result.put("message", "权限不存在");
            return result;
        }

        if (request.containsKey("permCode")) {
            permission.setPermCode((String) request.get("permCode"));
        }
        if (request.containsKey("permName")) {
            permission.setPermName((String) request.get("permName"));
        }
        permissionMapper.updateById(permission);

        result.put("code", 0);
        result.put("message", "ok");
        return result;
    }

    public Map<String, Object> searchPermissions(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();

        int page = parseIntOrDefault(params.get("page"), 1);
        int size = parseIntOrDefault(params.get("size"), 10);
        String keyword = (String) params.get("keyword");

        QueryWrapper<Permission> wrapper = new QueryWrapper<>();
        wrapper.eq("active", 1);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like("perm_code", keyword).or().like("perm_name", keyword));
        }
        wrapper.orderByAsc("perm_id");

        long total = permissionMapper.selectCount(wrapper);
        int offset = (page - 1) * size;
        wrapper.last("LIMIT " + offset + "," + size);
        List<Permission> permissions = permissionMapper.selectList(wrapper);

        List<Map<String, Object>> list = new ArrayList<>();
        for (Permission p : permissions) {
            Map<String, Object> item = new HashMap<>();
            item.put("permId", p.getPermId());
            item.put("permCode", p.getPermCode());
            item.put("permName", p.getPermName());
            item.put("active", p.getActive());
            list.add(item);
        }

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        result.put("data", data);
        return result;
    }

    // ==================== 分组权限管理 ====================

    public Map<String, Object> createGroupPermission(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        Object groupIdObj = request.get("groupId");
        Object permIdObj = request.get("permId");
        if (groupIdObj == null || permIdObj == null) {
            result.put("code", 400);
            result.put("message", "groupId 和 permId 必填");
            return result;
        }
        Long groupId = Long.valueOf(String.valueOf(groupIdObj));
        Long permId = Long.valueOf(String.valueOf(permIdObj));

        // 校验分组存在
        Group group = groupMapper.selectById(groupId);
        if (group == null || group.getIsDeleted() == 1) {
            result.put("code", 404);
            result.put("message", "分组不存在");
            return result;
        }

        // 校验权限存在
        Permission permission = permissionMapper.selectById(permId);
        if (permission == null || permission.getActive() == 0) {
            result.put("code", 404);
            result.put("message", "权限不存在");
            return result;
        }

        // 检查是否已关联
        QueryWrapper<GroupPermission> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id", groupId);
        wrapper.eq("perm_id", permId);
        if (groupPermissionMapper.selectOne(wrapper) != null) {
            result.put("code", 409);
            result.put("message", "该分组已关联此权限");
            return result;
        }

        GroupPermission gp = new GroupPermission();
        gp.setGroupId(groupId);
        gp.setPermId(permId);
        groupPermissionMapper.insert(gp);

        result.put("code", 0);
        result.put("message", "ok");
        return result;
    }

    public Map<String, Object> deleteGroupPermission(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        Object idObj = request.get("id");
        if (idObj == null) {
            result.put("code", 400);
            result.put("message", "id 必填");
            return result;
        }
        Long id = Long.valueOf(String.valueOf(idObj));

        GroupPermission gp = groupPermissionMapper.selectById(id);
        if (gp == null) {
            result.put("code", 404);
            result.put("message", "分组权限关联不存在");
            return result;
        }

        groupPermissionMapper.deleteById(id);

        result.put("code", 0);
        result.put("message", "ok");
        return result;
    }

    public Map<String, Object> updateGroupPermission(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        Object idObj = request.get("id");
        if (idObj == null) {
            result.put("code", 400);
            result.put("message", "id 必填");
            return result;
        }
        Long id = Long.valueOf(String.valueOf(idObj));

        GroupPermission gp = groupPermissionMapper.selectById(id);
        if (gp == null) {
            result.put("code", 404);
            result.put("message", "分组权限关联不存在");
            return result;
        }

        if (request.containsKey("permId")) {
            gp.setPermId(Long.valueOf(String.valueOf(request.get("permId"))));
        }
        groupPermissionMapper.updateById(gp);

        result.put("code", 0);
        result.put("message", "ok");
        return result;
    }

    public Map<String, Object> searchGroupPermissions(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();

        Object groupIdObj = params.get("groupId");
        if (groupIdObj == null) {
            result.put("code", 400);
            result.put("message", "groupId 必填");
            return result;
        }
        Long groupId = Long.valueOf(String.valueOf(groupIdObj));

        QueryWrapper<GroupPermission> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id", groupId);
        List<GroupPermission> gpList = groupPermissionMapper.selectList(wrapper);

        List<Map<String, Object>> list = new ArrayList<>();
        for (GroupPermission gp : gpList) {
            Permission p = permissionMapper.selectById(gp.getPermId());
            if (p != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", gp.getId());
                item.put("groupId", gp.getGroupId());
                item.put("permId", p.getPermId());
                item.put("permCode", p.getPermCode());
                item.put("permName", p.getPermName());
                list.add(item);
            }
        }

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        result.put("data", data);
        return result;
    }

    // ==================== 工具方法 ====================

    private int parseIntOrDefault(Object obj, int defaultValue) {
        if (obj == null) return defaultValue;
        try {
            return Integer.parseInt(String.valueOf(obj));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
