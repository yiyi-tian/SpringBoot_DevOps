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

    @Autowired
    private AttributeMapper attributeMapper;

    @Autowired
    private LoginHistoryMapper loginHistoryMapper;

    @Autowired
    private UserSessionMapper userSessionMapper;

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
        String code = (String) request.get("code"); // 验证码注册时，密码为空

        if (credentialType == null || credential == null || (password == null && code == null)) {
            result.put("code", 400);
            result.put("message", "参数不完整：credentialType, credential, password 必填");
            return result;
        }

        if (!credentialType.equals("PHONE") && !credentialType.equals("EMAIL") && !credentialType.equals("USERNAME")) {
            result.put("code", 400);
            result.put("message", "credentialType 必须为 PHONE / EMAIL / USERNAME");
            return result;
        }

        if (password != null && !password.isEmpty() && password.length() < 6) {
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
        userAuth.setSecretHash(password != null && !password.isEmpty() ? secretHash : "");
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
     * 用户登录（v0.4.0: 增加 IP 捕获、状态检查、登录历史记录、IP 突变检测）
     */
    public Map<String, Object> login(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        String credentialType = (String) request.get("credentialType");
        String credential = (String) request.get("credential");
        String password = (String) request.get("password");
        String code = (String) request.get("code"); // 验证码登录时，密码为空
        String clientIp = (String) request.get("clientIp");
        String userAgent = (String) request.get("userAgent");

        if (credentialType == null || credential == null || (password == null && code == null)) {
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

        if (password != null && !password.isEmpty() && !passwordEncoder.matches(password, userAuth.getSecretHash())) {
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

        // v0.4.0: 账户状态检查
        if ("LOCKED".equals(user.getStatus())) {
            result.put("code", 403);
            result.put("message", "账户已被锁定，请联系管理员");
            return result;
        }
        if ("RISKY".equals(user.getStatus())) {
            // RISKY 用户允许登录，但记录警告（实际审计日志由 topbiz 记录）
            System.err.println("[WARN] 风险用户登录: userId=" + user.getUserId() + ", ip=" + clientIp);
        }

        // 更新最后登录时间和 IP
        user.setLastLoginAt(LocalDateTime.now());
        if (clientIp != null && !clientIp.isEmpty()) {
            user.setLastLoginIp(clientIp);
        }
        userMapper.updateById(user);

        // v0.4.0: 记录登录历史
        LoginHistory history = new LoginHistory();
        history.setUserId(user.getUserId());
        history.setClientIp(clientIp != null ? clientIp : "unknown");
        history.setUserAgent(userAgent);
        history.setLoginAt(LocalDateTime.now());
        loginHistoryMapper.insert(history);

        // v0.4.0: 内联 IP 突变检测
        if (clientIp != null && !clientIp.isEmpty()) {
            checkIpMutation(user.getUserId(), clientIp);
        }

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("displayName", user.getDisplayName());
        result.put("data", data);
        return result;
    }

    // ==================== 用户自助服务 ====================

    /**
     * 修改密码（已知旧密码）
     */
    public Map<String, Object> changePassword(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        Object userIdObj = request.get("userId");
        String oldPassword = (String) request.get("oldPassword");
        String newPassword = (String) request.get("newPassword");

        if (userIdObj == null) {
            result.put("code", 400);
            result.put("message", "userId 必填");
            return result;
        }
        Long userId = Long.valueOf(String.valueOf(userIdObj));

        if (oldPassword == null || oldPassword.isEmpty()) {
            result.put("code", 400);
            result.put("message", "旧密码不能为空");
            return result;
        }
        if (newPassword == null || newPassword.length() < 6) {
            result.put("code", 400);
            result.put("message", "新密码长度不能少于6位");
            return result;
        }
        if (oldPassword.equals(newPassword)) {
            result.put("code", 400);
            result.put("message", "新密码不能与旧密码相同");
            return result;
        }

        // 查找用户的密码凭证
        QueryWrapper<UserAuth> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.isNotNull("secret_hash");
        List<UserAuth> auths = userAuthMapper.selectList(wrapper);

        if (auths.isEmpty()) {
            result.put("code", 404);
            result.put("message", "未找到可修改的密码凭证");
            return result;
        }

        // 校验旧密码
        UserAuth targetAuth = auths.get(0);
        if (!passwordEncoder.matches(oldPassword, targetAuth.getSecretHash())) {
            result.put("code", 401);
            result.put("message", "旧密码错误");
            return result;
        }

        // 更新所有密码凭证
        String newHash = passwordEncoder.encode(newPassword);
        for (UserAuth auth : auths) {
            auth.setSecretHash(newHash);
            userAuthMapper.updateById(auth);
        }

        result.put("code", 0);
        result.put("message", "ok");
        return result;
    }

    /**
     * 修改个人基本信息（displayName, sex）
     */
    public Map<String, Object> updateProfile(Map<String, Object> request) {
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
            String displayName = (String) request.get("displayName");
            if (displayName == null || displayName.trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "displayName 不能为空");
                return result;
            }
            user.setDisplayName(displayName.trim());
        }
        if (request.containsKey("sex")) {
            Object sexObj = request.get("sex");
            user.setSex(sexObj != null ? Integer.valueOf(String.valueOf(sexObj)) : null);
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("displayName", user.getDisplayName());
        data.put("sex", user.getSex());
        result.put("data", data);
        return result;
    }

    /**
     * 重置密码（忘记密码 — 验证码核验由 topbiz 完成）
     */
    public Map<String, Object> resetPassword(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        String credentialType = (String) request.get("credentialType");
        String credential = (String) request.get("credential");
        String newPassword = (String) request.get("newPassword");

        if (credentialType == null || credential == null) {
            result.put("code", 400);
            result.put("message", "credentialType 和 credential 必填");
            return result;
        }

        if (newPassword == null || newPassword.length() < 6) {
            result.put("code", 400);
            result.put("message", "新密码长度不能少于6位");
            return result;
        }

        // 查找凭证
        QueryWrapper<UserAuth> wrapper = new QueryWrapper<>();
        wrapper.eq("identity_type", credentialType);
        wrapper.eq("identifier", credential);
        UserAuth userAuth = userAuthMapper.selectOne(wrapper);

        if (userAuth == null) {
            result.put("code", 404);
            result.put("message", "凭证不存在");
            return result;
        }

        // 重置该凭证密码
        userAuth.setSecretHash(passwordEncoder.encode(newPassword));
        userAuthMapper.updateById(userAuth);

        result.put("code", 0);
        result.put("message", "ok");
        return result;
    }

    /**
     * 绑定新凭证到已有账号
     */
    public Map<String, Object> bindCredential(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        Object userIdObj = request.get("userId");
        String credentialType = (String) request.get("credentialType");
        String credential = (String) request.get("credential");
        String password = (String) request.get("password");

        if (userIdObj == null) {
            result.put("code", 400);
            result.put("message", "userId 必填");
            return result;
        }
        Long userId = Long.valueOf(String.valueOf(userIdObj));

        if (credentialType == null || credential == null) {
            result.put("code", 400);
            result.put("message", "credentialType 和 credential 必填");
            return result;
        }

        if (!credentialType.equals("PHONE") && !credentialType.equals("EMAIL") && !credentialType.equals("USERNAME")) {
            result.put("code", 400);
            result.put("message", "credentialType 必须为 PHONE / EMAIL / USERNAME");
            return result;
        }

        // 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            result.put("code", 404);
            result.put("message", "用户不存在");
            return result;
        }

        // 检查凭证是否已被使用
        QueryWrapper<UserAuth> authWrapper = new QueryWrapper<>();
        authWrapper.eq("identity_type", credentialType);
        authWrapper.eq("identifier", credential);
        if (userAuthMapper.selectOne(authWrapper) != null) {
            result.put("code", 409);
            result.put("message", "该凭证已被其他账号使用");
            return result;
        }

        // 创建新凭证
        UserAuth userAuth = new UserAuth();
        userAuth.setUserId(userId);
        userAuth.setIdentityType(credentialType);
        userAuth.setIdentifier(credential);
        if (password != null && !password.isEmpty()) {
            if (password != null && !password.isEmpty() && password.length() < 6) {
                result.put("code", 400);
                result.put("message", "密码长度不能少于6位");
                return result;
            }
            userAuth.setSecretHash(passwordEncoder.encode(password));
        }
        userAuth.setVerified(1);
        userAuth.setCreatedAt(LocalDateTime.now());
        userAuthMapper.insert(userAuth);

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("authId", userAuth.getAuthId());
        data.put("credentialType", credentialType);
        data.put("credential", credential);
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

        // 1. 直接权限（仅聚合 status=ACTIVE 的授权记录）
        QueryWrapper<UserPermission> upWrapper = new QueryWrapper<>();
        upWrapper.eq("user_id", userId);
        upWrapper.eq("status", "ACTIVE");
        for (UserPermission up : userPermissionMapper.selectList(upWrapper)) {
            Permission p = permissionMapper.selectById(up.getPermId());
            if (p != null && p.getActive() == 1) {
                permCodes.add(p.getPermCode());
            }
        }

        // 2. 通过分组的权限（仅聚合 status=ACTIVE 的授权记录）
        QueryWrapper<UserGroup> ugWrapper = new QueryWrapper<>();
        ugWrapper.eq("user_id", userId);
        for (UserGroup ug : userGroupMapper.selectList(ugWrapper)) {
            QueryWrapper<GroupPermission> gpWrapper = new QueryWrapper<>();
            gpWrapper.eq("group_id", ug.getGroupId());
            gpWrapper.eq("status", "ACTIVE");
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
        String code = (String) request.get("code"); // 验证码注册时，密码为空
        String displayName = (String) request.getOrDefault("displayName", credential);

        if (credentialType == null || credential == null || (password == null && code == null)) {
            result.put("code", 400);
            result.put("message", "参数不完整：credentialType, credential, password 必填");
            return result;
        }

        if (!credentialType.equals("PHONE") && !credentialType.equals("EMAIL") && !credentialType.equals("USERNAME")) {
            result.put("code", 400);
            result.put("message", "credentialType 必须为 PHONE / EMAIL / USERNAME");
            return result;
        }

        if (password != null && !password.isEmpty() && password.length() < 6) {
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
        userAuth.setSecretHash(password != null && !password.isEmpty() ? secretHash : "");
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

        // 计数查询（不含 ORDER BY，兼容 H2）
        QueryWrapper<User> countWrapper = new QueryWrapper<>();
        countWrapper.eq("is_deleted", 0);
        if (status != null && !status.isEmpty()) {
            countWrapper.eq("status", status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            countWrapper.like("display_name", keyword);
        }
        long total = userMapper.selectCount(countWrapper);

        // 列表查询（含排序 + 分页）
        QueryWrapper<User> listWrapper = new QueryWrapper<>();
        listWrapper.eq("is_deleted", 0);
        if (status != null && !status.isEmpty()) {
            listWrapper.eq("status", status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            listWrapper.like("display_name", keyword);
        }
        listWrapper.orderByDesc("created_at");
        int offset = (page - 1) * size;
        listWrapper.last("LIMIT " + offset + "," + size);
        List<User> users = userMapper.selectList(listWrapper);

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

        // 级联清理：移除该分组下所有用户关联
        QueryWrapper<UserGroup> ugWrapper = new QueryWrapper<>();
        ugWrapper.eq("group_id", groupId);
        userGroupMapper.delete(ugWrapper);

        // 级联清理：移除该分组下所有权限关联
        QueryWrapper<GroupPermission> gpWrapper = new QueryWrapper<>();
        gpWrapper.eq("group_id", groupId);
        groupPermissionMapper.delete(gpWrapper);

        // 逻辑删除分组
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

        // 计数查询（不含 ORDER BY，兼容 H2）
        QueryWrapper<Group> countWrapper = new QueryWrapper<>();
        countWrapper.eq("is_deleted", 0);
        if (keyword != null && !keyword.isEmpty()) {
            countWrapper.like("name", keyword);
        }
        long total = groupMapper.selectCount(countWrapper);

        // 列表查询（含排序 + 分页）
        QueryWrapper<Group> listWrapper = new QueryWrapper<>();
        listWrapper.eq("is_deleted", 0);
        if (keyword != null && !keyword.isEmpty()) {
            listWrapper.like("name", keyword);
        }
        listWrapper.orderByDesc("created_at");
        int offset = (page - 1) * size;
        listWrapper.last("LIMIT " + offset + "," + size);
        List<Group> groups = groupMapper.selectList(listWrapper);

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

        // 级联清理：移除该权限与所有分组的关联
        QueryWrapper<GroupPermission> gpWrapper = new QueryWrapper<>();
        gpWrapper.eq("perm_id", permId);
        groupPermissionMapper.delete(gpWrapper);

        // 级联清理：移除该权限与所有用户的直接关联
        QueryWrapper<UserPermission> upWrapper = new QueryWrapper<>();
        upWrapper.eq("perm_id", permId);
        userPermissionMapper.delete(upWrapper);

        // 停用权限
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

        // 计数查询（不含 ORDER BY，兼容 H2）
        QueryWrapper<Permission> countWrapper = new QueryWrapper<>();
        countWrapper.eq("active", 1);
        if (keyword != null && !keyword.isEmpty()) {
            countWrapper.and(w -> w.like("perm_code", keyword).or().like("perm_name", keyword));
        }
        long total = permissionMapper.selectCount(countWrapper);

        // 列表查询（含排序 + 分页）
        QueryWrapper<Permission> listWrapper = new QueryWrapper<>();
        listWrapper.eq("active", 1);
        if (keyword != null && !keyword.isEmpty()) {
            listWrapper.and(w -> w.like("perm_code", keyword).or().like("perm_name", keyword));
        }
        listWrapper.orderByAsc("perm_id");
        int offset = (page - 1) * size;
        listWrapper.last("LIMIT " + offset + "," + size);
        List<Permission> permissions = permissionMapper.selectList(listWrapper);

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
        gp.setStatus("ACTIVE");
        gp.setCreatedAt(LocalDateTime.now());
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

    // ==================== 用户直接权限管理 ====================

    public Map<String, Object> createUserPermission(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        Object userIdObj = request.get("userId");
        Object permIdObj = request.get("permId");
        if (userIdObj == null || permIdObj == null) {
            result.put("code", 400);
            result.put("message", "userId 和 permId 必填");
            return result;
        }
        Long userId = Long.valueOf(String.valueOf(userIdObj));
        Long permId = Long.valueOf(String.valueOf(permIdObj));

        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            result.put("code", 404);
            result.put("message", "用户不存在");
            return result;
        }

        Permission permission = permissionMapper.selectById(permId);
        if (permission == null || permission.getActive() == 0) {
            result.put("code", 404);
            result.put("message", "权限不存在");
            return result;
        }

        QueryWrapper<UserPermission> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("perm_id", permId);
        if (userPermissionMapper.selectOne(wrapper) != null) {
            result.put("code", 409);
            result.put("message", "用户已拥有该权限");
            return result;
        }

        UserPermission up = new UserPermission();
        up.setUserId(userId);
        up.setPermId(permId);
        up.setStatus("ACTIVE");
        up.setCreatedAt(LocalDateTime.now());
        userPermissionMapper.insert(up);

        result.put("code", 0);
        result.put("message", "ok");
        return result;
    }

    public Map<String, Object> deleteUserPermission(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        Object idObj = request.get("id");
        if (idObj == null) {
            result.put("code", 400);
            result.put("message", "id 必填");
            return result;
        }
        Long id = Long.valueOf(String.valueOf(idObj));

        UserPermission up = userPermissionMapper.selectById(id);
        if (up == null) {
            result.put("code", 404);
            result.put("message", "用户权限关联不存在");
            return result;
        }

        userPermissionMapper.deleteById(id);

        result.put("code", 0);
        result.put("message", "ok");
        return result;
    }

    public Map<String, Object> updateUserPermission(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        Object idObj = request.get("id");
        if (idObj == null) {
            result.put("code", 400);
            result.put("message", "id 必填");
            return result;
        }
        Long id = Long.valueOf(String.valueOf(idObj));

        UserPermission up = userPermissionMapper.selectById(id);
        if (up == null) {
            result.put("code", 404);
            result.put("message", "用户权限关联不存在");
            return result;
        }

        if (request.containsKey("permId")) {
            Long newPermId = Long.valueOf(String.valueOf(request.get("permId")));
            Permission permission = permissionMapper.selectById(newPermId);
            if (permission == null || permission.getActive() == 0) {
                result.put("code", 404);
                result.put("message", "权限不存在");
                return result;
            }
            up.setPermId(newPermId);
        }
        userPermissionMapper.updateById(up);

        result.put("code", 0);
        result.put("message", "ok");
        return result;
    }

    public Map<String, Object> searchUserPermissions(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();

        Object userIdObj = params.get("userId");
        if (userIdObj == null) {
            result.put("code", 400);
            result.put("message", "userId 必填");
            return result;
        }
        Long userId = Long.valueOf(String.valueOf(userIdObj));

        QueryWrapper<UserPermission> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        List<UserPermission> upList = userPermissionMapper.selectList(wrapper);

        List<Map<String, Object>> list = new ArrayList<>();
        for (UserPermission up : upList) {
            Permission p = permissionMapper.selectById(up.getPermId());
            if (p != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", up.getId());
                item.put("userId", up.getUserId());
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

    // ==================== 权限申请与审批 ====================

    /**
     * 用户申请权限（1.1.11）
     * 三步拦截：Token校验（由 topbiz 完成）→ 查重已有权限(ACTIVE) → 查重在途申请(PENDING)
     */
    public Map<String, Object> applyUserPermission(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        Object userIdObj = request.get("userId");
        Object permIdObj = request.get("permId");
        if (userIdObj == null || permIdObj == null) {
            result.put("code", 400);
            result.put("message", "userId 和 permId 必填");
            return result;
        }
        Long userId = Long.valueOf(String.valueOf(userIdObj));
        Long permId = Long.valueOf(String.valueOf(permIdObj));

        // 校验用户存在
        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            result.put("code", 404);
            result.put("message", "用户不存在");
            return result;
        }

        // 校验权限存在
        Permission permission = permissionMapper.selectById(permId);
        if (permission == null || permission.getActive() == 0) {
            result.put("code", 404);
            result.put("message", "权限不存在");
            return result;
        }

        // 第一步：查重已有权限（status=ACTIVE）
        QueryWrapper<UserPermission> activeWrapper = new QueryWrapper<>();
        activeWrapper.eq("user_id", userId);
        activeWrapper.eq("perm_id", permId);
        activeWrapper.eq("status", "ACTIVE");
        if (userPermissionMapper.selectOne(activeWrapper) != null) {
            result.put("code", 409);
            result.put("message", "您已拥有该权限，无需重复申请");
            return result;
        }

        // 第二步：查重在途申请（status=PENDING）
        QueryWrapper<UserPermission> pendingWrapper = new QueryWrapper<>();
        pendingWrapper.eq("user_id", userId);
        pendingWrapper.eq("perm_id", permId);
        pendingWrapper.eq("status", "PENDING");
        if (userPermissionMapper.selectOne(pendingWrapper) != null) {
            result.put("code", 409);
            result.put("message", "您已提交过该权限的申请，请等待审批");
            return result;
        }

        // 第三步：创建申请记录（status=PENDING）
        UserPermission up = new UserPermission();
        up.setUserId(userId);
        up.setPermId(permId);
        up.setStatus("PENDING");
        up.setCreatedAt(LocalDateTime.now());
        userPermissionMapper.insert(up);

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("id", up.getId());
        data.put("status", "PENDING");
        result.put("data", data);
        return result;
    }

    /**
     * 管理员审批通过用户权限申请
     */
    public Map<String, Object> approveUserPermission(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        Object idObj = request.get("id");
        if (idObj == null) {
            result.put("code", 400);
            result.put("message", "id 必填");
            return result;
        }
        Long id = Long.valueOf(String.valueOf(idObj));

        UserPermission up = userPermissionMapper.selectById(id);
        if (up == null) {
            result.put("code", 404);
            result.put("message", "申请记录不存在");
            return result;
        }

        if (!"PENDING".equals(up.getStatus())) {
            result.put("code", 400);
            result.put("message", "该申请状态为 " + up.getStatus() + "，无法审批");
            return result;
        }

        up.setStatus("ACTIVE");
        userPermissionMapper.updateById(up);

        result.put("code", 0);
        result.put("message", "ok");
        return result;
    }

    /**
     * 管理员驳回用户权限申请
     */
    public Map<String, Object> rejectUserPermission(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        Object idObj = request.get("id");
        if (idObj == null) {
            result.put("code", 400);
            result.put("message", "id 必填");
            return result;
        }
        Long id = Long.valueOf(String.valueOf(idObj));

        UserPermission up = userPermissionMapper.selectById(id);
        if (up == null) {
            result.put("code", 404);
            result.put("message", "申请记录不存在");
            return result;
        }

        if (!"PENDING".equals(up.getStatus())) {
            result.put("code", 400);
            result.put("message", "该申请状态为 " + up.getStatus() + "，无法审批");
            return result;
        }

        up.setStatus("REJECTED");
        userPermissionMapper.updateById(up);

        result.put("code", 0);
        result.put("message", "ok");
        return result;
    }

    /**
     * 组申请权限（1.1.13.5）
     * 逻辑与用户申请权限对称：查重 ACTIVE → 查重 PENDING → 创建 PENDING 记录
     */
    public Map<String, Object> applyGroupPermission(Map<String, Object> request) {
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

        // 第一步：查重已有权限（status=ACTIVE）
        QueryWrapper<GroupPermission> activeWrapper = new QueryWrapper<>();
        activeWrapper.eq("group_id", groupId);
        activeWrapper.eq("perm_id", permId);
        activeWrapper.eq("status", "ACTIVE");
        if (groupPermissionMapper.selectOne(activeWrapper) != null) {
            result.put("code", 409);
            result.put("message", "该分组已拥有此权限，无需重复申请");
            return result;
        }

        // 第二步：查重在途申请（status=PENDING）
        QueryWrapper<GroupPermission> pendingWrapper = new QueryWrapper<>();
        pendingWrapper.eq("group_id", groupId);
        pendingWrapper.eq("perm_id", permId);
        pendingWrapper.eq("status", "PENDING");
        if (groupPermissionMapper.selectOne(pendingWrapper) != null) {
            result.put("code", 409);
            result.put("message", "该分组已提交过此权限的申请，请等待审批");
            return result;
        }

        // 第三步：创建申请记录（status=PENDING）
        GroupPermission gp = new GroupPermission();
        gp.setGroupId(groupId);
        gp.setPermId(permId);
        gp.setStatus("PENDING");
        gp.setCreatedAt(LocalDateTime.now());
        groupPermissionMapper.insert(gp);

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("id", gp.getId());
        data.put("status", "PENDING");
        result.put("data", data);
        return result;
    }

    /**
     * 管理员审批通过组权限申请
     */
    public Map<String, Object> approveGroupPermission(Map<String, Object> request) {
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
            result.put("message", "申请记录不存在");
            return result;
        }

        if (!"PENDING".equals(gp.getStatus())) {
            result.put("code", 400);
            result.put("message", "该申请状态为 " + gp.getStatus() + "，无法审批");
            return result;
        }

        gp.setStatus("ACTIVE");
        groupPermissionMapper.updateById(gp);

        result.put("code", 0);
        result.put("message", "ok");
        return result;
    }

    /**
     * 管理员驳回组权限申请
     */
    public Map<String, Object> rejectGroupPermission(Map<String, Object> request) {
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
            result.put("message", "申请记录不存在");
            return result;
        }

        if (!"PENDING".equals(gp.getStatus())) {
            result.put("code", 400);
            result.put("message", "该申请状态为 " + gp.getStatus() + "，无法审批");
            return result;
        }

        gp.setStatus("REJECTED");
        groupPermissionMapper.updateById(gp);

        result.put("code", 0);
        result.put("message", "ok");
        return result;
    }

    // ==================== 自驱任务 ====================

    /**
     * 认证过期检测：扫描 user_auth.expired_at 超过阈值的用户，标记状态为 EXPIRED
     */
    public Map<String, Object> expireStaleAuths(int days) {
        Map<String, Object> result = new HashMap<>();
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);

        QueryWrapper<UserAuth> authWrapper = new QueryWrapper<>();
        authWrapper.isNotNull("expired_at");
        authWrapper.lt("expired_at", threshold);
        List<UserAuth> expiredAuths = userAuthMapper.selectList(authWrapper);

        Set<Long> affectedUserIds = new LinkedHashSet<>();
        for (UserAuth auth : expiredAuths) {
            User user = userMapper.selectById(auth.getUserId());
            if (user != null && user.getIsDeleted() == 0 && "ACTIVE".equals(user.getStatus())) {
                user.setStatus("EXPIRED");
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.updateById(user);
                affectedUserIds.add(user.getUserId());
            }
        }

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("affectedCount", affectedUserIds.size());
        data.put("threshold", threshold.toString());
        result.put("data", data);
        return result;
    }

    /**
     * 长期未活跃用户识别：扫描 last_login_at 超过阈值的活跃用户，标记为 INACTIVE
     */
    public Map<String, Object> deactivateInactiveUsers(int days) {
        Map<String, Object> result = new HashMap<>();
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);

        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0);
        wrapper.eq("status", "ACTIVE");
        wrapper.and(w -> w.isNull("last_login_at").or().lt("last_login_at", threshold));
        List<User> inactiveUsers = userMapper.selectList(wrapper);

        int count = 0;
        for (User user : inactiveUsers) {
            user.setStatus("INACTIVE");
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
            count++;
        }

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("affectedCount", count);
        data.put("threshold", threshold.toString());
        result.put("data", data);
        return result;
    }

    /**
     * 物理删除已注销用户（超过保留期限）
     */
    public Map<String, Object> purgeDeregisteredUsers(int days) {
        Map<String, Object> result = new HashMap<>();
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);

        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "DEREGISTERED");
        wrapper.lt("updated_at", threshold);
        List<User> deregisteredUsers = userMapper.selectList(wrapper);

        int count = 0;
        for (User user : deregisteredUsers) {
            physicalDeleteUser(user.getUserId());
            count++;
        }

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("deletedCount", count);
        data.put("threshold", threshold.toString());
        result.put("data", data);
        return result;
    }

    /**
     * 物理删除用户及其所有关联数据
     */
    private void physicalDeleteUser(Long userId) {
        QueryWrapper<UserAuth> authWrapper = new QueryWrapper<>();
        authWrapper.eq("user_id", userId);
        userAuthMapper.delete(authWrapper);

        QueryWrapper<Attribute> attrWrapper = new QueryWrapper<>();
        attrWrapper.eq("user_id", userId);
        attributeMapper.delete(attrWrapper);

        QueryWrapper<UserGroup> ugWrapper = new QueryWrapper<>();
        ugWrapper.eq("user_id", userId);
        userGroupMapper.delete(ugWrapper);

        QueryWrapper<UserPermission> upWrapper = new QueryWrapper<>();
        upWrapper.eq("user_id", userId);
        userPermissionMapper.delete(upWrapper);

        userMapper.deleteById(userId);
    }

    // ==================== 数据清理任务 ====================

    /**
     * 清理用户过期缓存（1.2.3.1）
     * 建立缓存键命名规范与版本管理策略。
     * 当前 Shiro 会话缓存由 Redis TTL 自动管理，此方法预留为应用级缓存清理入口。
     *
     * 缓存键命名规范：
     *   user:perm:v{version}:{userId}    — 用户权限缓存
     *   user:profile:v{version}:{userId} — 用户信息缓存
     *   shiro:session:{sessionId}        — Shiro 会话（由 Spring Session 管理）
     *
     * 版本轮换时，旧版本缓存由 Redis TTL 自动过期，无需主动删除。
     */
    public Map<String, Object> cleanExpiredCaches() {
        Map<String, Object> result = new HashMap<>();
        // Shiro 会话缓存由 Redis TTL 自动管理，应用级缓存尚未大量使用
        // 此端点预留为缓存版本升级时的旧版 Key 清理入口
        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("note", "Redis TTL 自动管理会话过期；应用级缓存启用后可通过此端点触发版本清理");
        data.put("cacheKeyConventions", new String[]{
                "user:perm:v{version}:{userId}",
                "user:profile:v{version}:{userId}",
                "shiro:session:{sessionId}"
        });
        result.put("data", data);
        return result;
    }

    /**
     * 清理用户过期数据（1.2.3.3）
     * 物理删除超过保留期限的已驳回权限申请记录，防止 junction 表无限增长。
     *
     * @param retentionDays 保留天数（默认 90 天），超过此期限的 REJECTED 记录将被物理删除
     */
    public Map<String, Object> cleanExpiredData(int retentionDays) {
        Map<String, Object> result = new HashMap<>();
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);

        // 1. 清理过期的 REJECTED 用户权限申请记录
        QueryWrapper<UserPermission> upWrapper = new QueryWrapper<>();
        upWrapper.eq("status", "REJECTED");
        upWrapper.lt("created_at", threshold);
        long upDeleted = userPermissionMapper.selectCount(upWrapper);
        if (upDeleted > 0) {
            userPermissionMapper.delete(upWrapper);
        }

        // 2. 清理过期的 REJECTED 组权限申请记录
        QueryWrapper<GroupPermission> gpWrapper = new QueryWrapper<>();
        gpWrapper.eq("status", "REJECTED");
        gpWrapper.lt("created_at", threshold);
        long gpDeleted = groupPermissionMapper.selectCount(gpWrapper);
        if (gpDeleted > 0) {
            groupPermissionMapper.delete(gpWrapper);
        }

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("deletedUserPermissions", upDeleted);
        data.put("deletedGroupPermissions", gpDeleted);
        data.put("retentionDays", retentionDays);
        data.put("threshold", threshold.toString());
        result.put("data", data);
        return result;
    }

    // ==================== v0.4.0: IP 突变检测 & 多端会话管理 ====================

    /**
     * 内联 IP 突变检测（登录时调用）
     * 查询最近 5 条登录历史，统计最近 24 小时内的不同 /16 子网数。
     * ≥3 → 标记 RISKY；≥5 → 标记 LOCKED。
     */
    private void checkIpMutation(Long userId, String currentIp) {
        String currentSubnet = extractSubnet(currentIp);
        if (currentSubnet == null) return;

        LocalDateTime since = LocalDateTime.now().minusHours(24);

        // 查询最近 5 条登录历史
        QueryWrapper<LoginHistory> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.orderByDesc("login_at");
        wrapper.last("LIMIT 5");
        List<LoginHistory> recentLogins = loginHistoryMapper.selectList(wrapper);

        // 统计 24 小时内不同的 /16 子网
        Set<String> subnets = new LinkedHashSet<>();
        subnets.add(currentSubnet);
        for (LoginHistory lh : recentLogins) {
            if (lh.getLoginAt() != null && lh.getLoginAt().isAfter(since)) {
                String sn = extractSubnet(lh.getClientIp());
                if (sn != null) subnets.add(sn);
            }
        }

        int distinctSubnets = subnets.size();
        if (distinctSubnets >= 5) {
            // 锁定账户
            User user = userMapper.selectById(userId);
            if (user != null && !"LOCKED".equals(user.getStatus())) {
                user.setStatus("LOCKED");
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.updateById(user);
                System.err.println("[SECURITY] 用户 " + userId + " 24h 内从 " + distinctSubnets
                        + " 个不同 /16 子网登录，已自动锁定。子网: " + subnets);
            }
        } else if (distinctSubnets >= 3) {
            // 标记为风险
            User user = userMapper.selectById(userId);
            if (user != null && "ACTIVE".equals(user.getStatus())) {
                user.setStatus("RISKY");
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.updateById(user);
                System.err.println("[SECURITY] 用户 " + userId + " 24h 内从 " + distinctSubnets
                        + " 个不同 /16 子网登录，已标记 RISKY。子网: " + subnets);
            }
        }
    }

    /**
     * 提取 IPv4 的 /16 子网前缀（前两个 octet）
     * 例: "192.168.1.100" → "192.168"
     */
    private String extractSubnet(String ip) {
        if (ip == null || ip.isEmpty()) return null;
        String[] parts = ip.split("\\.");
        if (parts.length >= 2) {
            return parts[0] + "." + parts[1];
        }
        return null;
    }

    /**
     * IP 突变定时扫描（1.2.2.2 调度任务）
     * 扫描 login_history，对 24h 内从多个 /16 子网登录的用户标记 RISKY/LOCKED。
     */
    public Map<String, Object> scanIpMutations(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        int hours = request.containsKey("hours") ? parseIntOrDefault(request.get("hours"), 24) : 24;
        LocalDateTime since = LocalDateTime.now().minusHours(hours);

        // 查询所有在时间窗口内的登录历史
        QueryWrapper<LoginHistory> wrapper = new QueryWrapper<>();
        wrapper.ge("login_at", since);
        wrapper.orderByAsc("user_id");
        List<LoginHistory> allLogins = loginHistoryMapper.selectList(wrapper);

        // 按 userId 分组统计不同子网
        Map<Long, Set<String>> userSubnets = new LinkedHashMap<>();
        for (LoginHistory lh : allLogins) {
            String subnet = extractSubnet(lh.getClientIp());
            if (subnet != null) {
                userSubnets.computeIfAbsent(lh.getUserId(), k -> new LinkedHashSet<>()).add(subnet);
            }
        }

        int riskyCount = 0;
        int lockedCount = 0;
        for (Map.Entry<Long, Set<String>> entry : userSubnets.entrySet()) {
            Long userId = entry.getKey();
            int distinct = entry.getValue().size();

            User user = userMapper.selectById(userId);
            if (user == null || user.getIsDeleted() == 1) continue;

            if (distinct >= 5 && !"LOCKED".equals(user.getStatus())) {
                user.setStatus("LOCKED");
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.updateById(user);
                lockedCount++;
            } else if (distinct >= 3 && "ACTIVE".equals(user.getStatus())) {
                user.setStatus("RISKY");
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.updateById(user);
                riskyCount++;
            }
        }

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("riskyCount", riskyCount);
        data.put("lockedCount", lockedCount);
        data.put("scannedUsers", userSubnets.size());
        data.put("hours", hours);
        result.put("data", data);
        return result;
    }

    // ==================== v0.4.0: 多端会话管理 ====================

    /**
     * 注册用户会话（登录成功后调用）
     */
    public Map<String, Object> registerSession(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        Object userIdObj = request.get("userId");
        String sessionId = (String) request.get("sessionId");
        String deviceType = (String) request.get("deviceType");
        String clientIp = (String) request.get("clientIp");
        String userAgent = (String) request.get("userAgent");

        if (userIdObj == null || sessionId == null) {
            result.put("code", 400);
            result.put("message", "userId 和 sessionId 必填");
            return result;
        }

        Long userId = Long.valueOf(String.valueOf(userIdObj));

        // 检查是否已有该 sessionId 的记录（防止重复注册）
        QueryWrapper<UserSession> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId);
        if (userSessionMapper.selectCount(wrapper) > 0) {
            result.put("code", 0);
            result.put("message", "session already registered");
            return result;
        }

        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setSessionId(sessionId);
        session.setDeviceType(deviceType != null ? deviceType : "UNKNOWN");
        session.setClientIp(clientIp);
        session.setUserAgent(userAgent);
        session.setLoginAt(LocalDateTime.now());
        session.setLastActiveAt(LocalDateTime.now());
        session.setStatus("ACTIVE");
        userSessionMapper.insert(session);

        result.put("code", 0);
        result.put("message", "ok");
        return result;
    }

    /**
     * 查询用户的所有活跃会话
     */
    public Map<String, Object> getUserSessions(Long userId) {
        Map<String, Object> result = new HashMap<>();

        QueryWrapper<UserSession> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("status", "ACTIVE");
        wrapper.orderByDesc("login_at");
        List<UserSession> sessions = userSessionMapper.selectList(wrapper);

        List<Map<String, Object>> sessionList = new ArrayList<>();
        for (UserSession s : sessions) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", s.getId());
            item.put("sessionId", s.getSessionId());
            item.put("deviceType", s.getDeviceType());
            item.put("clientIp", s.getClientIp());
            item.put("userAgent", s.getUserAgent());
            item.put("loginAt", s.getLoginAt() != null ? s.getLoginAt().toString() : null);
            item.put("lastActiveAt", s.getLastActiveAt() != null ? s.getLastActiveAt().toString() : null);
            sessionList.add(item);
        }

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("sessions", sessionList);
        data.put("count", sessionList.size());
        result.put("data", data);
        return result;
    }

    /**
     * 终止指定会话（标记为 TERMINATED）
     */
    public Map<String, Object> terminateSession(String sessionId) {
        Map<String, Object> result = new HashMap<>();

        QueryWrapper<UserSession> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId);
        UserSession session = userSessionMapper.selectOne(wrapper);

        if (session == null) {
            result.put("code", 404);
            result.put("message", "会话不存在");
            return result;
        }

        session.setStatus("TERMINATED");
        session.setLastActiveAt(LocalDateTime.now());
        userSessionMapper.updateById(session);

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", sessionId);
        data.put("userId", session.getUserId());
        result.put("data", data);
        return result;
    }

    /**
     * 终止用户除当前会话外的所有其他活跃会话
     */
    public Map<String, Object> terminateOtherSessions(Long userId, String currentSessionId) {
        Map<String, Object> result = new HashMap<>();

        QueryWrapper<UserSession> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("status", "ACTIVE");
        wrapper.ne("session_id", currentSessionId);
        List<UserSession> others = userSessionMapper.selectList(wrapper);

        int count = 0;
        for (UserSession s : others) {
            s.setStatus("TERMINATED");
            s.setLastActiveAt(LocalDateTime.now());
            userSessionMapper.updateById(s);
            count++;
        }

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("terminatedCount", count);
        result.put("data", data);
        return result;
    }

    /**
     * 清理过期会话（2.1.5 调度任务）
     * 将创建超过 sessionTtlMinutes 分钟且仍为 ACTIVE 的会话标记为 EXPIRED。
     */
    public Map<String, Object> cleanStaleSessions(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        int ttlMinutes = request.containsKey("ttlMinutes") ? parseIntOrDefault(request.get("ttlMinutes"), 30) : 30;
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(ttlMinutes);

        QueryWrapper<UserSession> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "ACTIVE");
        wrapper.lt("login_at", threshold);
        List<UserSession> staleSessions = userSessionMapper.selectList(wrapper);

        int count = 0;
        for (UserSession s : staleSessions) {
            s.setStatus("EXPIRED");
            s.setLastActiveAt(LocalDateTime.now());
            userSessionMapper.updateById(s);
            count++;
        }

        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("expiredCount", count);
        data.put("ttlMinutes", ttlMinutes);
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
