package org.example.userservice;

import org.example.userservice.entity.*;
import org.example.userservice.mapper.*;
import org.example.userservice.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserService 服务层测试
 * 使用 H2 内存数据库 + MyBatis-Plus，每个测试方法独立验证业务逻辑。
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserServiceTest {

    @Autowired
    private UserService userService;

    // ---- 辅助方法：注册用户，返回 userId ----
    private Long registerUser(String type, String identifier, String password) {
        Map<String, Object> req = new HashMap<>();
        req.put("credentialType", type);
        req.put("credential", identifier);
        req.put("password", password);
        Map<String, Object> resp = userService.register(req);
        assertEquals(0, resp.get("code"), "注册应成功: " + resp.get("message"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        return ((Number) data.get("userId")).longValue();
    }

    // ---- 辅助方法：创建权限，返回 permId ----
    private Long createPerm(String code, String name) {
        Map<String, Object> req = new HashMap<>();
        req.put("permCode", code);
        req.put("permName", name);
        Map<String, Object> resp = userService.createPermission(req);
        assertEquals(0, resp.get("code"), "创建权限应成功: " + resp.get("message"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        return ((Number) data.get("permId")).longValue();
    }

    // ---- 辅助方法：创建分组，返回 groupId ----
    private Long createGroup(String name, Long creatorUserId) {
        Map<String, Object> req = new HashMap<>();
        req.put("name", name);
        req.put("creatorUserId", creatorUserId);
        Map<String, Object> resp = userService.createGroup(req);
        assertEquals(0, resp.get("code"), "创建分组应成功: " + resp.get("message"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        return ((Number) data.get("groupId")).longValue();
    }

    // ============================ 注册 ============================

    @Test
    @Order(1)
    void testRegisterSuccess() {
        Map<String, Object> req = new HashMap<>();
        req.put("credentialType", "USERNAME");
        req.put("credential", "testuser");
        req.put("password", "123456");
        Map<String, Object> resp = userService.register(req);
        assertEquals(0, resp.get("code"));
        assertNotNull(resp.get("data"));
    }

    @Test
    @Order(2)
    void testRegisterDuplicate() {
        // 使用与 testRegisterSuccess 相同的凭证
        Map<String, Object> req = new HashMap<>();
        req.put("credentialType", "USERNAME");
        req.put("credential", "testuser");
        req.put("password", "123456");
        Map<String, Object> resp = userService.register(req);
        assertEquals(409, resp.get("code"));
        assertEquals("该凭证已被注册", resp.get("message"));
    }

    @Test
    @Order(3)
    void testRegisterInvalidCredentialType() {
        Map<String, Object> req = new HashMap<>();
        req.put("credentialType", "QQ");
        req.put("credential", "12345");
        req.put("password", "123456");
        Map<String, Object> resp = userService.register(req);
        assertEquals(400, resp.get("code"));
    }

    @Test
    @Order(4)
    void testRegisterShortPassword() {
        Map<String, Object> req = new HashMap<>();
        req.put("credentialType", "USERNAME");
        req.put("credential", "shortpwd");
        req.put("password", "123");
        Map<String, Object> resp = userService.register(req);
        assertEquals(400, resp.get("code"));
        assertTrue(((String) resp.get("message")).contains("密码长度"));
    }

    @Test
    @Order(5)
    void testRegisterMissingParams() {
        Map<String, Object> req = new HashMap<>();
        req.put("credentialType", "USERNAME");
        Map<String, Object> resp = userService.register(req);
        assertEquals(400, resp.get("code"));
    }

    @Test
    @Order(6)
    void testRegisterWithEmail() {
        Map<String, Object> req = new HashMap<>();
        req.put("credentialType", "EMAIL");
        req.put("credential", "user@example.com");
        req.put("password", "password123");
        Map<String, Object> resp = userService.register(req);
        assertEquals(0, resp.get("code"));
    }

    @Test
    @Order(7)
    void testRegisterWithPhone() {
        Map<String, Object> req = new HashMap<>();
        req.put("credentialType", "PHONE");
        req.put("credential", "13800138000");
        req.put("password", "password123");
        Map<String, Object> resp = userService.register(req);
        assertEquals(0, resp.get("code"));
    }

    // ============================ 登录 ============================

    @Test
    @Order(10)
    void testLoginSuccess() {
        // 先注册
        Long userId = registerUser("USERNAME", "loginuser", "pass123");
        assertNotNull(userId);

        Map<String, Object> req = new HashMap<>();
        req.put("credentialType", "USERNAME");
        req.put("credential", "loginuser");
        req.put("password", "pass123");
        Map<String, Object> resp = userService.login(req);
        assertEquals(0, resp.get("code"), "登录应成功: " + resp.get("message"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        assertEquals(userId, ((Number) data.get("userId")).longValue());
    }

    @Test
    @Order(11)
    void testLoginWrongPassword() {
        registerUser("USERNAME", "wrongpwduser", "correct");
        Map<String, Object> req = new HashMap<>();
        req.put("credentialType", "USERNAME");
        req.put("credential", "wrongpwduser");
        req.put("password", "wrongpassword");
        Map<String, Object> resp = userService.login(req);
        assertEquals(401, resp.get("code"));
        assertEquals("密码错误", resp.get("message"));
    }

    @Test
    @Order(12)
    void testLoginNonExistentCredential() {
        Map<String, Object> req = new HashMap<>();
        req.put("credentialType", "USERNAME");
        req.put("credential", "nonexistent");
        req.put("password", "whatever");
        Map<String, Object> resp = userService.login(req);
        assertEquals(401, resp.get("code"));
        assertEquals("凭证不存在", resp.get("message"));
    }

    @Test
    @Order(13)
    void testLoginMissingParams() {
        Map<String, Object> req = new HashMap<>();
        req.put("credentialType", "USERNAME");
        Map<String, Object> resp = userService.login(req);
        assertEquals(400, resp.get("code"));
    }

    @Test
    @Order(14)
    void testLoginWithIpAndUserAgent() {
        Long userId = registerUser("USERNAME", "ipuser", "pass123");
        Map<String, Object> req = new HashMap<>();
        req.put("credentialType", "USERNAME");
        req.put("credential", "ipuser");
        req.put("password", "pass123");
        req.put("clientIp", "192.168.1.100");
        req.put("userAgent", "Mozilla/5.0 TestBrowser");
        Map<String, Object> resp = userService.login(req);
        assertEquals(0, resp.get("code"));
    }

    // ============================ 修改密码 ============================

    @Test
    @Order(20)
    void testChangePasswordSuccess() {
        Long userId = registerUser("USERNAME", "changepwd", "oldpass");
        Map<String, Object> req = new HashMap<>();
        req.put("userId", userId);
        req.put("oldPassword", "oldpass");
        req.put("newPassword", "newpass123");
        Map<String, Object> resp = userService.changePassword(req);
        assertEquals(0, resp.get("code"), "修改密码应成功: " + resp.get("message"));

        // 验证新密码可登录
        Map<String, Object> loginReq = new HashMap<>();
        loginReq.put("credentialType", "USERNAME");
        loginReq.put("credential", "changepwd");
        loginReq.put("password", "newpass123");
        assertEquals(0, userService.login(loginReq).get("code"));
    }

    @Test
    @Order(21)
    void testChangePasswordWrongOldPassword() {
        Long userId = registerUser("USERNAME", "wrongold", "correctpwd");
        Map<String, Object> req = new HashMap<>();
        req.put("userId", userId);
        req.put("oldPassword", "wrongpwd");
        req.put("newPassword", "newpass456");
        Map<String, Object> resp = userService.changePassword(req);
        assertEquals(401, resp.get("code"));
    }

    @Test
    @Order(22)
    void testChangePasswordSameAsOld() {
        Long userId = registerUser("USERNAME", "samepwd", "samepass");
        Map<String, Object> req = new HashMap<>();
        req.put("userId", userId);
        req.put("oldPassword", "samepass");
        req.put("newPassword", "samepass");
        Map<String, Object> resp = userService.changePassword(req);
        assertEquals(400, resp.get("code"));
        assertTrue(((String) resp.get("message")).contains("不能与旧密码相同"));
    }

    @Test
    @Order(23)
    void testChangePasswordTooShort() {
        Long userId = registerUser("USERNAME", "shortnew", "oldpass");
        Map<String, Object> req = new HashMap<>();
        req.put("userId", userId);
        req.put("oldPassword", "oldpass");
        req.put("newPassword", "123");
        Map<String, Object> resp = userService.changePassword(req);
        assertEquals(400, resp.get("code"));
        assertTrue(((String) resp.get("message")).contains("6位"));
    }

    // ============================ 更新个人资料 ============================

    @Test
    @Order(30)
    void testUpdateProfileSuccess() {
        Long userId = registerUser("USERNAME", "profileuser", "pass123");
        Map<String, Object> req = new HashMap<>();
        req.put("userId", userId);
        req.put("displayName", "张三");
        req.put("sex", 1);
        Map<String, Object> resp = userService.updateProfile(req);
        assertEquals(0, resp.get("code"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        assertEquals("张三", data.get("displayName"));
        assertEquals(1, data.get("sex"));
    }

    @Test
    @Order(31)
    void testUpdateProfileUserNotFound() {
        Map<String, Object> req = new HashMap<>();
        req.put("userId", 99999L);
        req.put("displayName", "Ghost");
        Map<String, Object> resp = userService.updateProfile(req);
        assertEquals(404, resp.get("code"));
    }

    // ============================ 注销 / 登出 ============================

    @Test
    @Order(40)
    void testDeregisterSuccess() {
        Long userId = registerUser("USERNAME", "dereguser", "pass123");
        Map<String, Object> resp = userService.deregister(userId);
        assertEquals(0, resp.get("code"));
    }

    @Test
    @Order(41)
    void testDeregisterNotFound() {
        Map<String, Object> resp = userService.deregister(99999L);
        assertEquals(404, resp.get("code"));
    }

    @Test
    @Order(42)
    void testLogout() {
        Long userId = registerUser("USERNAME", "logoutuser", "pass123");
        Map<String, Object> resp = userService.logout(userId);
        assertEquals(0, resp.get("code"));
    }

    // ============================ 管理员创建用户 ============================

    @Test
    @Order(50)
    void testAdminCreateUser() {
        Map<String, Object> req = new HashMap<>();
        req.put("credentialType", "USERNAME");
        req.put("credential", "admincreated");
        req.put("password", "admin123");
        req.put("displayName", "管理员创建的用户");
        Map<String, Object> resp = userService.createUser(req);
        assertEquals(0, resp.get("code"));
    }

    @Test
    @Order(51)
    void testAdminCreateUserDuplicate() {
        Map<String, Object> req = new HashMap<>();
        req.put("credentialType", "USERNAME");
        req.put("credential", "admincreated");
        req.put("password", "admin123");
        Map<String, Object> resp = userService.createUser(req);
        assertEquals(409, resp.get("code"));
    }

    // ============================ 删除/更新/搜索用户 ============================

    @Test
    @Order(55)
    void testDeleteUserSuccess() {
        Long userId = registerUser("USERNAME", "deleteuser", "pass123");
        Map<String, Object> req = new HashMap<>();
        req.put("userId", userId);
        Map<String, Object> resp = userService.deleteUser(req);
        assertEquals(0, resp.get("code"));
    }

    @Test
    @Order(56)
    void testDeleteUserNotFound() {
        Map<String, Object> req = new HashMap<>();
        req.put("userId", 99999L);
        Map<String, Object> resp = userService.deleteUser(req);
        assertEquals(404, resp.get("code"));
    }

    @Test
    @Order(57)
    void testUpdateUser() {
        Long userId = registerUser("USERNAME", "updateuser", "pass123");
        Map<String, Object> req = new HashMap<>();
        req.put("userId", userId);
        req.put("displayName", "已更新名称");
        req.put("status", "ACTIVE");
        Map<String, Object> resp = userService.updateUser(req);
        assertEquals(0, resp.get("code"));
    }

    @Test
    @Order(58)
    void testSearchUsers() {
        registerUser("USERNAME", "searchuser1", "pass123");
        registerUser("EMAIL", "search2@test.com", "pass123");

        Map<String, Object> params = new HashMap<>();
        params.put("page", 1);
        params.put("size", 10);
        Map<String, Object> resp = userService.searchUsers(params);
        assertEquals(0, resp.get("code"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        assertTrue(((Number) data.get("total")).longValue() >= 2);
    }

    @Test
    @Order(59)
    void testSearchUsersWithKeyword() {
        registerUser("USERNAME", "keyworduser", "pass123");
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", "keyword");
        params.put("page", 1);
        params.put("size", 10);
        Map<String, Object> resp = userService.searchUsers(params);
        assertEquals(0, resp.get("code"));
    }

    // ============================ 分组管理 ============================

    @Test
    @Order(60)
    void testCreateGroup() {
        Long creatorId = registerUser("USERNAME", "groupcreator", "pass123");
        Map<String, Object> req = new HashMap<>();
        req.put("name", "测试分组");
        req.put("description", "用于测试的分组");
        req.put("creatorUserId", creatorId);
        Map<String, Object> resp = userService.createGroup(req);
        assertEquals(0, resp.get("code"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        assertNotNull(data.get("groupId"));
    }

    @Test
    @Order(61)
    void testCreateGroupMissingName() {
        Map<String, Object> req = new HashMap<>();
        req.put("name", "");
        Map<String, Object> resp = userService.createGroup(req);
        assertEquals(400, resp.get("code"));
    }

    @Test
    @Order(62)
    void testDeleteGroupWithCascade() {
        Long creatorId = registerUser("USERNAME", "groupdelcreator", "pass123");
        Long groupId = createGroup("待删除分组", creatorId);

        // 向分组添加用户
        Map<String, Object> addReq = new HashMap<>();
        addReq.put("userId", creatorId);
        addReq.put("groupId", groupId);
        userService.addUserToGroup(addReq);

        // 关联权限
        Long permId = createPerm("group:del:test", "删除测试权限");
        Map<String, Object> gpReq = new HashMap<>();
        gpReq.put("groupId", groupId);
        gpReq.put("permId", permId);
        userService.createGroupPermission(gpReq);

        // 删除分组（级联清理）
        Map<String, Object> delReq = new HashMap<>();
        delReq.put("groupId", groupId);
        Map<String, Object> resp = userService.deleteGroup(delReq);
        assertEquals(0, resp.get("code"));
    }

    @Test
    @Order(63)
    void testUpdateGroup() {
        Long creatorId = registerUser("USERNAME", "groupupdatecr", "pass123");
        Long groupId = createGroup("原名称", creatorId);
        Map<String, Object> req = new HashMap<>();
        req.put("groupId", groupId);
        req.put("name", "新名称");
        req.put("description", "新描述");
        Map<String, Object> resp = userService.updateGroup(req);
        assertEquals(0, resp.get("code"));
    }

    @Test
    @Order(64)
    void testSearchGroups() {
        Long creatorId = registerUser("USERNAME", "groupsearchcr", "pass123");
        createGroup("搜索分组A", creatorId);
        createGroup("搜索分组B", creatorId);
        Map<String, Object> params = new HashMap<>();
        params.put("page", 1);
        params.put("size", 10);
        Map<String, Object> resp = userService.searchGroups(params);
        assertEquals(0, resp.get("code"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        assertTrue(((Number) data.get("total")).longValue() >= 2);
    }

    // ============================ 用户-分组管理 ============================

    @Test
    @Order(70)
    void testAddUserToGroup() {
        Long userId = registerUser("USERNAME", "groupmember", "pass123");
        Long groupId = createGroup("成员组", userId);
        Map<String, Object> req = new HashMap<>();
        req.put("userId", userId);
        req.put("groupId", groupId);
        Map<String, Object> resp = userService.addUserToGroup(req);
        assertEquals(0, resp.get("code"));
    }

    @Test
    @Order(71)
    void testAddUserToGroupDuplicate() {
        Long userId = registerUser("USERNAME", "dupgroupmember", "pass123");
        Long groupId = createGroup("重复成员组", userId);
        Map<String, Object> req = new HashMap<>();
        req.put("userId", userId);
        req.put("groupId", groupId);
        userService.addUserToGroup(req);
        Map<String, Object> resp = userService.addUserToGroup(req);
        assertEquals(409, resp.get("code"));
    }

    @Test
    @Order(72)
    void testRemoveUserFromGroup() {
        Long userId = registerUser("USERNAME", "removegroupmember", "pass123");
        Long groupId = createGroup("移除测试组", userId);
        Map<String, Object> addReq = new HashMap<>();
        addReq.put("userId", userId);
        addReq.put("groupId", groupId);
        userService.addUserToGroup(addReq);

        Map<String, Object> delReq = new HashMap<>();
        delReq.put("userId", userId);
        delReq.put("groupId", groupId);
        Map<String, Object> resp = userService.removeUserFromGroup(delReq);
        assertEquals(0, resp.get("code"));
    }

    @Test
    @Order(73)
    void testSearchGroupUsers() {
        Long userId = registerUser("USERNAME", "groupusersearch", "pass123");
        Long groupId = createGroup("用户搜索组", userId);
        Map<String, Object> addReq = new HashMap<>();
        addReq.put("userId", userId);
        addReq.put("groupId", groupId);
        userService.addUserToGroup(addReq);

        Map<String, Object> params = new HashMap<>();
        params.put("groupId", groupId);
        params.put("page", 1);
        params.put("size", 10);
        Map<String, Object> resp = userService.searchGroupUsers(params);
        assertEquals(0, resp.get("code"));
    }

    // ============================ 权限管理 ============================

    @Test
    @Order(80)
    void testCreatePermission() {
        Map<String, Object> req = new HashMap<>();
        req.put("permCode", "user:read");
        req.put("permName", "读取用户");
        Map<String, Object> resp = userService.createPermission(req);
        assertEquals(0, resp.get("code"));
    }

    @Test
    @Order(81)
    void testCreatePermissionDuplicateCode() {
        Map<String, Object> req = new HashMap<>();
        req.put("permCode", "user:read");
        req.put("permName", "读取用户(v2)");
        Map<String, Object> resp = userService.createPermission(req);
        assertEquals(409, resp.get("code"));
    }

    @Test
    @Order(82)
    void testCreatePermissionMissingParams() {
        Map<String, Object> req = new HashMap<>();
        req.put("permCode", "missing:name");
        Map<String, Object> resp = userService.createPermission(req);
        assertEquals(400, resp.get("code"));
    }

    @Test
    @Order(83)
    void testDeletePermission() {
        Long permId = createPerm("temp:perm", "临时权限");
        Map<String, Object> req = new HashMap<>();
        req.put("permId", permId);
        Map<String, Object> resp = userService.deletePermission(req);
        assertEquals(0, resp.get("code"));
    }

    @Test
    @Order(84)
    void testUpdatePermission() {
        Long permId = createPerm("update:perm", "旧名称");
        Map<String, Object> req = new HashMap<>();
        req.put("permId", permId);
        req.put("permName", "新名称");
        req.put("permCode", "update:perm:new");
        Map<String, Object> resp = userService.updatePermission(req);
        assertEquals(0, resp.get("code"));
    }

    @Test
    @Order(85)
    void testSearchPermissions() {
        createPerm("search:perm1", "搜索权限1");
        createPerm("search:perm2", "搜索权限2");
        Map<String, Object> params = new HashMap<>();
        params.put("page", 1);
        params.put("size", 10);
        Map<String, Object> resp = userService.searchPermissions(params);
        assertEquals(0, resp.get("code"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        assertTrue(((Number) data.get("total")).longValue() >= 2);
    }

    // ============================ 分组-权限管理 ============================

    @Test
    @Order(90)
    void testCreateGroupPermission() {
        Long userId = registerUser("USERNAME", "grouppermuser", "pass123");
        Long groupId = createGroup("权限组", userId);
        Long permId = createPerm("group:perm:test", "分组权限测试");
        Map<String, Object> req = new HashMap<>();
        req.put("groupId", groupId);
        req.put("permId", permId);
        Map<String, Object> resp = userService.createGroupPermission(req);
        assertEquals(0, resp.get("code"));
    }

    @Test
    @Order(91)
    void testCreateGroupPermissionDuplicate() {
        Long userId = registerUser("USERNAME", "dupgrpperm", "pass123");
        Long groupId = createGroup("重复权限组", userId);
        Long permId = createPerm("group:dup:perm", "重复分组权限");
        Map<String, Object> req = new HashMap<>();
        req.put("groupId", groupId);
        req.put("permId", permId);
        userService.createGroupPermission(req);
        assertEquals(409, userService.createGroupPermission(req).get("code"));
    }

    @Test
    @Order(92)
    void testDeleteGroupPermission() {
        Long userId = registerUser("USERNAME", "delgrpperm", "pass123");
        Long groupId = createGroup("删除权限组", userId);
        Long permId = createPerm("group:del:perm2", "删除分组权限2");
        Map<String, Object> createReq = new HashMap<>();
        createReq.put("groupId", groupId);
        createReq.put("permId", permId);
        userService.createGroupPermission(createReq);

        // 获取 id
        Map<String, Object> searchReq = new HashMap<>();
        searchReq.put("groupId", groupId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>)
                ((Map<String, Object>) userService.searchGroupPermissions(searchReq).get("data")).get("list");
        Long id = ((Number) list.get(0).get("id")).longValue();

        Map<String, Object> delReq = new HashMap<>();
        delReq.put("id", id);
        assertEquals(0, userService.deleteGroupPermission(delReq).get("code"));
    }

    // ============================ 用户直接权限 ============================

    @Test
    @Order(100)
    void testCreateUserPermission() {
        Long userId = registerUser("USERNAME", "userpermuser", "pass123");
        Long permId = createPerm("user:direct:perm", "直接权限");
        Map<String, Object> req = new HashMap<>();
        req.put("userId", userId);
        req.put("permId", permId);
        Map<String, Object> resp = userService.createUserPermission(req);
        assertEquals(0, resp.get("code"));
    }

    @Test
    @Order(101)
    void testDeleteUserPermission() {
        Long userId = registerUser("USERNAME", "deluserperm", "pass123");
        Long permId = createPerm("user:del:perm", "待删除权限");
        Map<String, Object> createReq = new HashMap<>();
        createReq.put("userId", userId);
        createReq.put("permId", permId);
        userService.createUserPermission(createReq);

        Map<String, Object> searchReq = new HashMap<>();
        searchReq.put("userId", userId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>)
                ((Map<String, Object>) userService.searchUserPermissions(searchReq).get("data")).get("list");
        Long id = ((Number) list.get(0).get("id")).longValue();

        Map<String, Object> delReq = new HashMap<>();
        delReq.put("id", id);
        assertEquals(0, userService.deleteUserPermission(delReq).get("code"));
    }

    @Test
    @Order(102)
    void testGetPermissionsWithGroupInheritance() {
        Long userId = registerUser("USERNAME", "inheritance", "pass123");
        Long directPermId = createPerm("direct:perm", "直接权限");
        Long groupPermId = createPerm("group:inherited", "分组继承权限");

        // 直接授予
        Map<String, Object> directReq = new HashMap<>();
        directReq.put("userId", userId);
        directReq.put("permId", directPermId);
        userService.createUserPermission(directReq);

        // 通过分组
        Long groupId = createGroup("权限继承组", userId);
        Map<String, Object> gpReq = new HashMap<>();
        gpReq.put("groupId", groupId);
        gpReq.put("permId", groupPermId);
        userService.createGroupPermission(gpReq);

        Map<String, Object> addReq = new HashMap<>();
        addReq.put("userId", userId);
        addReq.put("groupId", groupId);
        userService.addUserToGroup(addReq);

        // 查询聚合权限
        Map<String, Object> resp = userService.getPermissions(userId);
        assertEquals(0, resp.get("code"));
        @SuppressWarnings("unchecked")
        List<String> perms = (List<String>) ((Map<String, Object>) resp.get("data")).get("permissions");
        assertTrue(perms.contains("direct:perm"));
        assertTrue(perms.contains("group:inherited"));
    }

    @Test
    @Order(103)
    void testGetGroups() {
        Long userId = registerUser("USERNAME", "getgroupsuser", "pass123");
        Long groupId1 = createGroup("分组1", userId);
        Long groupId2 = createGroup("分组2", userId);

        Map<String, Object> req1 = new HashMap<>();
        req1.put("userId", userId);
        req1.put("groupId", groupId1);
        userService.addUserToGroup(req1);

        Map<String, Object> req2 = new HashMap<>();
        req2.put("userId", userId);
        req2.put("groupId", groupId2);
        userService.addUserToGroup(req2);

        Map<String, Object> resp = userService.getGroups(userId);
        assertEquals(0, resp.get("code"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> groups = (List<Map<String, Object>>)
                ((Map<String, Object>) resp.get("data")).get("groups");
        assertEquals(2, groups.size());
    }

    // ============================ 权限申请与审批 ============================

    @Test
    @Order(110)
    void testApplyUserPermissionSuccess() {
        Long userId = registerUser("USERNAME", "applyuser", "pass123");
        Long permId = createPerm("apply:perm", "申请权限");
        Map<String, Object> req = new HashMap<>();
        req.put("userId", userId);
        req.put("permId", permId);
        Map<String, Object> resp = userService.applyUserPermission(req);
        assertEquals(0, resp.get("code"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        assertEquals("PENDING", data.get("status"));
        return;
    }

    @Test
    @Order(111)
    void testApplyUserPermissionDuplicateActive() {
        Long userId = registerUser("USERNAME", "applydupactive", "pass123");
        Long permId = createPerm("apply:dup:active", "重复申请已存在");
        Map<String, Object> directReq = new HashMap<>();
        directReq.put("userId", userId);
        directReq.put("permId", permId);
        userService.createUserPermission(directReq);

        Map<String, Object> req = new HashMap<>();
        req.put("userId", userId);
        req.put("permId", permId);
        Map<String, Object> resp = userService.applyUserPermission(req);
        assertEquals(409, resp.get("code"));
        assertTrue(((String) resp.get("message")).contains("已拥有"));
    }

    @Test
    @Order(112)
    void testApplyUserPermissionDuplicatePending() {
        Long userId = registerUser("USERNAME", "applyduppend", "pass123");
        Long permId = createPerm("apply:dup:pending", "重复申请在途");
        Map<String, Object> req = new HashMap<>();
        req.put("userId", userId);
        req.put("permId", permId);
        userService.applyUserPermission(req);
        // 再次申请
        Map<String, Object> resp = userService.applyUserPermission(req);
        assertEquals(409, resp.get("code"));
        assertTrue(((String) resp.get("message")).contains("等待审批"));
    }

    @Test
    @Order(113)
    void testApproveUserPermission() {
        Long userId = registerUser("USERNAME", "approveuser", "pass123");
        Long permId = createPerm("approve:perm", "审批权限");
        Map<String, Object> applyReq = new HashMap<>();
        applyReq.put("userId", userId);
        applyReq.put("permId", permId);
        Map<String, Object> applyResp = userService.applyUserPermission(applyReq);
        @SuppressWarnings("unchecked")
        Long id = ((Number) ((Map<String, Object>) applyResp.get("data")).get("id")).longValue();

        Map<String, Object> approveReq = new HashMap<>();
        approveReq.put("id", id);
        Map<String, Object> resp = userService.approveUserPermission(approveReq);
        assertEquals(0, resp.get("code"));
    }

    @Test
    @Order(114)
    void testRejectUserPermission() {
        Long userId = registerUser("USERNAME", "rejectuser", "pass123");
        Long permId = createPerm("reject:perm", "驳回权限");
        Map<String, Object> applyReq = new HashMap<>();
        applyReq.put("userId", userId);
        applyReq.put("permId", permId);
        Map<String, Object> applyResp = userService.applyUserPermission(applyReq);
        @SuppressWarnings("unchecked")
        Long id = ((Number) ((Map<String, Object>) applyResp.get("data")).get("id")).longValue();

        Map<String, Object> rejectReq = new HashMap<>();
        rejectReq.put("id", id);
        Map<String, Object> resp = userService.rejectUserPermission(rejectReq);
        assertEquals(0, resp.get("code"));
    }

    @Test
    @Order(115)
    void testApproveNonPendingThrows() {
        Map<String, Object> req = new HashMap<>();
        req.put("id", 99999L);
        Map<String, Object> resp = userService.approveUserPermission(req);
        assertEquals(404, resp.get("code"));
    }

    @Test
    @Order(116)
    void testRejectNonPendingThrows() {
        Map<String, Object> req = new HashMap<>();
        req.put("id", 99999L);
        Map<String, Object> resp = userService.rejectUserPermission(req);
        assertEquals(404, resp.get("code"));
    }

    // ============================ 组权限申请与审批 ============================

    @Test
    @Order(120)
    void testApplyGroupPermissionSuccess() {
        Long userId = registerUser("USERNAME", "gpapplyuser", "pass123");
        Long groupId = createGroup("申请权限组", userId);
        Long permId = createPerm("gp:apply:perm", "组申请权限");
        Map<String, Object> req = new HashMap<>();
        req.put("groupId", groupId);
        req.put("permId", permId);
        Map<String, Object> resp = userService.applyGroupPermission(req);
        assertEquals(0, resp.get("code"));
    }

    @Test
    @Order(121)
    void testApproveGroupPermission() {
        Long userId = registerUser("USERNAME", "gpapproveuser", "pass123");
        Long groupId = createGroup("审批组权限组", userId);
        Long permId = createPerm("gp:approve:perm", "组审批权限");
        Map<String, Object> applyReq = new HashMap<>();
        applyReq.put("groupId", groupId);
        applyReq.put("permId", permId);
        Map<String, Object> applyResp = userService.applyGroupPermission(applyReq);
        @SuppressWarnings("unchecked")
        Long id = ((Number) ((Map<String, Object>) applyResp.get("data")).get("id")).longValue();

        Map<String, Object> approveReq = new HashMap<>();
        approveReq.put("id", id);
        assertEquals(0, userService.approveGroupPermission(approveReq).get("code"));
    }

    @Test
    @Order(122)
    void testRejectGroupPermission() {
        Long userId = registerUser("USERNAME", "gprejectuser", "pass123");
        Long groupId = createGroup("驳回组权限组", userId);
        Long permId = createPerm("gp:reject:perm", "组驳回权限");
        Map<String, Object> applyReq = new HashMap<>();
        applyReq.put("groupId", groupId);
        applyReq.put("permId", permId);
        Map<String, Object> applyResp = userService.applyGroupPermission(applyReq);
        @SuppressWarnings("unchecked")
        Long id = ((Number) ((Map<String, Object>) applyResp.get("data")).get("id")).longValue();

        Map<String, Object> rejectReq = new HashMap<>();
        rejectReq.put("id", id);
        assertEquals(0, userService.rejectGroupPermission(rejectReq).get("code"));
    }

    // ============================ 自驱任务 ============================

    @Test
    @Order(130)
    void testExpireStaleAuths() {
        Map<String, Object> resp = userService.expireStaleAuths(14);
        assertEquals(0, resp.get("code"));
    }

    @Test
    @Order(131)
    void testDeactivateInactiveUsers() {
        Map<String, Object> resp = userService.deactivateInactiveUsers(30);
        assertEquals(0, resp.get("code"));
    }

    @Test
    @Order(132)
    void testPurgeDeregisteredUsers() {
        Map<String, Object> resp = userService.purgeDeregisteredUsers(30);
        assertEquals(0, resp.get("code"));
    }

    @Test
    @Order(133)
    void testCleanExpiredCaches() {
        Map<String, Object> resp = userService.cleanExpiredCaches();
        assertEquals(0, resp.get("code"));
    }

    @Test
    @Order(134)
    void testCleanExpiredData() {
        Map<String, Object> resp = userService.cleanExpiredData(90);
        assertEquals(0, resp.get("code"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        assertEquals(90, data.get("retentionDays"));
    }

    // ============================ v0.4.0: 会话管理 ============================

    @Test
    @Order(140)
    void testRegisterSession() {
        Long userId = registerUser("USERNAME", "sessionuser", "pass123");
        Map<String, Object> req = new HashMap<>();
        req.put("userId", userId);
        req.put("sessionId", "session-abc-123");
        req.put("deviceType", "WEB");
        req.put("clientIp", "10.0.0.1");
        req.put("userAgent", "Chrome/120");
        Map<String, Object> resp = userService.registerSession(req);
        assertEquals(0, resp.get("code"));
    }

    @Test
    @Order(141)
    void testRegisterSessionDuplicate() {
        Long userId = registerUser("USERNAME", "dupsession", "pass123");
        Map<String, Object> req = new HashMap<>();
        req.put("userId", userId);
        req.put("sessionId", "session-dup-456");
        userService.registerSession(req);
        // 重复注册应返回成功，但不重复插入
        Map<String, Object> resp = userService.registerSession(req);
        assertEquals(0, resp.get("code"));
    }

    @Test
    @Order(142)
    void testGetUserSessions() {
        Long userId = registerUser("USERNAME", "getsessions", "pass123");
        Map<String, Object> req = new HashMap<>();
        req.put("userId", userId);
        req.put("sessionId", "session-get-1");
        req.put("deviceType", "ANDROID");
        userService.registerSession(req);

        Map<String, Object> resp = userService.getUserSessions(userId);
        assertEquals(0, resp.get("code"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        assertTrue(((Number) data.get("count")).intValue() >= 1);
    }

    @Test
    @Order(143)
    void testTerminateSession() {
        Long userId = registerUser("USERNAME", "terminatesession", "pass123");
        Map<String, Object> req = new HashMap<>();
        req.put("userId", userId);
        req.put("sessionId", "session-term-789");
        userService.registerSession(req);

        Map<String, Object> resp = userService.terminateSession("session-term-789");
        assertEquals(0, resp.get("code"));
    }

    @Test
    @Order(144)
    void testTerminateSessionNotFound() {
        Map<String, Object> resp = userService.terminateSession("nonexistent-session");
        assertEquals(404, resp.get("code"));
    }

    @Test
    @Order(145)
    void testTerminateOtherSessions() {
        Long userId = registerUser("USERNAME", "terminateother", "pass123");
        Map<String, Object> s1 = new HashMap<>();
        s1.put("userId", userId);
        s1.put("sessionId", "keep-session");
        userService.registerSession(s1);

        Map<String, Object> s2 = new HashMap<>();
        s2.put("userId", userId);
        s2.put("sessionId", "other-session-1");
        userService.registerSession(s2);

        Map<String, Object> s3 = new HashMap<>();
        s3.put("userId", userId);
        s3.put("sessionId", "other-session-2");
        userService.registerSession(s3);

        Map<String, Object> resp = userService.terminateOtherSessions(userId, "keep-session");
        assertEquals(0, resp.get("code"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        assertTrue(((Number) data.get("terminatedCount")).intValue() >= 1);
    }

    @Test
    @Order(146)
    void testCleanStaleSessions() {
        Long userId = registerUser("USERNAME", "staleclean", "pass123");
        Map<String, Object> req = new HashMap<>();
        req.put("userId", userId);
        req.put("sessionId", "stale-session");
        userService.registerSession(req);

        Map<String, Object> cleanReq = new HashMap<>();
        cleanReq.put("ttlMinutes", 30);
        Map<String, Object> resp = userService.cleanStaleSessions(cleanReq);
        assertEquals(0, resp.get("code"));
    }

    // ============================ v0.4.0: IP 突变 ============================

    @Test
    @Order(150)
    void testScanIpMutations() {
        Map<String, Object> req = new HashMap<>();
        req.put("hours", 24);
        Map<String, Object> resp = userService.scanIpMutations(req);
        assertEquals(0, resp.get("code"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        assertNotNull(data.get("scannedUsers"));
        assertNotNull(data.get("riskyCount"));
        assertNotNull(data.get("lockedCount"));
    }

    @Test
    @Order(151)
    void testLoginWithIpMutationDetection() {
        // 从不同 /16 子网登录会触发 IP 突变检测（内部调用 checkIpMutation）
        Long userId = registerUser("USERNAME", "ipmutation", "pass123");
        Map<String, Object> req = new HashMap<>();
        req.put("credentialType", "USERNAME");
        req.put("credential", "ipmutation");
        req.put("password", "pass123");
        req.put("clientIp", "10.0.0.1");
        Map<String, Object> resp = userService.login(req);
        assertEquals(0, resp.get("code"));
    }

    // ============================ 重置密码 ============================

    @Test
    @Order(160)
    void testResetPassword() {
        registerUser("USERNAME", "resetpwduser", "oldpass");
        Map<String, Object> req = new HashMap<>();
        req.put("credentialType", "USERNAME");
        req.put("credential", "resetpwduser");
        req.put("newPassword", "newresetpass");
        Map<String, Object> resp = userService.resetPassword(req);
        assertEquals(0, resp.get("code"));

        // 验证新密码可登录
        Map<String, Object> loginReq = new HashMap<>();
        loginReq.put("credentialType", "USERNAME");
        loginReq.put("credential", "resetpwduser");
        loginReq.put("password", "newresetpass");
        assertEquals(0, userService.login(loginReq).get("code"));
    }

    @Test
    @Order(161)
    void testResetPasswordCredentialNotFound() {
        Map<String, Object> req = new HashMap<>();
        req.put("credentialType", "USERNAME");
        req.put("credential", "noone");
        req.put("newPassword", "newpass");
        Map<String, Object> resp = userService.resetPassword(req);
        assertEquals(404, resp.get("code"));
    }

    // ============================ 绑定凭证 ============================

    @Test
    @Order(170)
    void testBindCredentialSuccess() {
        Long userId = registerUser("USERNAME", "binduser", "pass123");
        Map<String, Object> req = new HashMap<>();
        req.put("userId", userId);
        req.put("credentialType", "EMAIL");
        req.put("credential", "bind@example.com");
        req.put("password", "emailpass");
        Map<String, Object> resp = userService.bindCredential(req);
        assertEquals(0, resp.get("code"));
    }

    @Test
    @Order(171)
    void testBindCredentialDuplicate() {
        Long userId = registerUser("USERNAME", "binddupuser", "pass123");
        Map<String, Object> req = new HashMap<>();
        req.put("userId", userId);
        req.put("credentialType", "EMAIL");
        req.put("credential", "dupbind@example.com");
        userService.bindCredential(req);
        Map<String, Object> resp = userService.bindCredential(req);
        assertEquals(409, resp.get("code"));
    }
}
