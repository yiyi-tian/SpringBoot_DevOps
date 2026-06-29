package org.example.userservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController 控制器层集成测试
 * 通过 MockMvc 测试完整的 HTTP 请求 → Service → 响应链路。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Long testUserId;
    private static Long testPermId;
    private static Long testGroupId;

    // ============================ 注册 ============================

    @Test
    @Order(1)
    void testRegister() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("credentialType", "USERNAME");
        body.put("credential", "controlleruser");
        body.put("password", "controller123");

        String resp = mockMvc.perform(post("/internal/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").exists())
                .andReturn().getResponse().getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> respMap = objectMapper.readValue(resp, Map.class);
        testUserId = ((Number) ((Map<String, Object>) respMap.get("data")).get("userId")).longValue();
    }

    @Test
    @Order(2)
    void testRegisterDuplicate() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("credentialType", "USERNAME");
        body.put("credential", "controlleruser");
        body.put("password", "controller123");

        mockMvc.perform(post("/internal/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    @Order(3)
    void testRegisterInvalidType() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("credentialType", "WECHAT");
        body.put("credential", "wx_openid_123");
        body.put("password", "pass123456");

        mockMvc.perform(post("/internal/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ============================ 登录 ============================

    @Test
    @Order(4)
    void testLogin() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("credentialType", "USERNAME");
        body.put("credential", "controlleruser");
        body.put("password", "controller123");
        body.put("clientIp", "192.168.1.1");
        body.put("userAgent", "JUnit/MockMvc");

        mockMvc.perform(post("/internal/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(testUserId.intValue()));
    }

    @Test
    @Order(5)
    void testLoginWrongPassword() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("credentialType", "USERNAME");
        body.put("credential", "controlleruser");
        body.put("password", "wrongpassword");

        mockMvc.perform(post("/internal/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // ============================ 用户自助服务 ============================

    @Test
    @Order(6)
    void testChangePassword() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", testUserId);
        body.put("oldPassword", "controller123");
        body.put("newPassword", "newcontroller123");

        mockMvc.perform(post("/internal/user/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @Order(7)
    void testUpdateProfile() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", testUserId);
        body.put("displayName", "控制器用户");

        mockMvc.perform(patch("/internal/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.displayName").value("控制器用户"));
    }

    @Test
    @Order(8)
    void testResetPassword() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("credentialType", "USERNAME");
        body.put("credential", "controlleruser");
        body.put("newPassword", "resetcontroller");

        mockMvc.perform(post("/internal/user/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @Order(9)
    void testBindCredential() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", testUserId);
        body.put("credentialType", "EMAIL");
        body.put("credential", "controller@test.com");
        body.put("password", "emailpassword");

        mockMvc.perform(post("/internal/user/bind")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    // ============================ 用户生命周期 ============================

    @Test
    @Order(10)
    void testGetPermissions() throws Exception {
        mockMvc.perform(get("/internal/user/{userId}/permissions", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.permissions").exists());
    }

    @Test
    @Order(11)
    void testGetGroups() throws Exception {
        mockMvc.perform(get("/internal/user/{userId}/groups", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.groups").exists());
    }

    // ============================ 管理员：用户管理 ============================

    @Test
    @Order(12)
    void testAdminCreateUser() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("credentialType", "USERNAME");
        body.put("credential", "admin_created");
        body.put("password", "admin123456");
        body.put("displayName", "管理员创建");

        mockMvc.perform(post("/internal/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").exists());
    }

    @Test
    @Order(13)
    void testSearchUsers() throws Exception {
        mockMvc.perform(get("/internal/user/search")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @Order(14)
    void testUpdateUser() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", testUserId);
        body.put("displayName", "更新后的控制器用户");

        mockMvc.perform(patch("/internal/user/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    // ============================ 分组管理 ============================

    @Test
    @Order(15)
    void testCreateGroup() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "控制器测试分组");
        body.put("description", "通过控制器创建的测试分组");
        body.put("creatorUserId", testUserId);

        String resp = mockMvc.perform(post("/internal/group/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.groupId").exists())
                .andReturn().getResponse().getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> respMap = objectMapper.readValue(resp, Map.class);
        testGroupId = ((Number) ((Map<String, Object>) respMap.get("data")).get("groupId")).longValue();
    }

    @Test
    @Order(16)
    void testSearchGroups() throws Exception {
        mockMvc.perform(get("/internal/group/search")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @Order(17)
    void testUpdateGroup() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("groupId", testGroupId);
        body.put("name", "更新后的分组名");

        mockMvc.perform(patch("/internal/group/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    // ============================ 用户-分组管理 ============================

    @Test
    @Order(18)
    void testAddUserToGroup() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", testUserId);
        body.put("groupId", testGroupId);

        mockMvc.perform(post("/internal/group-user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @Order(19)
    void testAddUserToGroupDuplicate() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", testUserId);
        body.put("groupId", testGroupId);

        mockMvc.perform(post("/internal/group-user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    @Order(20)
    void testSearchGroupUsers() throws Exception {
        mockMvc.perform(get("/internal/group-user/search")
                        .param("groupId", String.valueOf(testGroupId))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @Order(21)
    void testRemoveUserFromGroup() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", testUserId);
        body.put("groupId", testGroupId);

        mockMvc.perform(delete("/internal/group-user/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    // ============================ 权限管理 ============================

    @Test
    @Order(22)
    void testCreatePermission() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("permCode", "controller:perm");
        body.put("permName", "控制器测试权限");

        String resp = mockMvc.perform(post("/internal/permission/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.permId").exists())
                .andReturn().getResponse().getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> respMap = objectMapper.readValue(resp, Map.class);
        testPermId = ((Number) ((Map<String, Object>) respMap.get("data")).get("permId")).longValue();
    }

    @Test
    @Order(23)
    void testSearchPermissions() throws Exception {
        mockMvc.perform(get("/internal/permission/search")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @Order(24)
    void testUpdatePermission() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("permId", testPermId);
        body.put("permName", "更新后的权限名");

        mockMvc.perform(patch("/internal/permission/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    // ============================ 分组-权限管理 ============================

    @Test
    @Order(25)
    void testCreateGroupPermission() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("groupId", testGroupId);
        body.put("permId", testPermId);

        mockMvc.perform(post("/internal/group-permission/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @Order(26)
    void testSearchGroupPermissions() throws Exception {
        mockMvc.perform(get("/internal/group-permission/search")
                        .param("groupId", String.valueOf(testGroupId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    // ============================ 用户-权限管理 ============================

    @Test
    @Order(27)
    void testCreateUserPermission() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", testUserId);
        body.put("permId", testPermId);

        mockMvc.perform(post("/internal/user-permission/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @Order(28)
    void testSearchUserPermissions() throws Exception {
        mockMvc.perform(get("/internal/user-permission/search")
                        .param("userId", String.valueOf(testUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    // ============================ 权限申请与审批 ============================

    @Test
    @Order(29)
    void testApplyUserPermission() throws Exception {
        // 先创建一个新权限用于申请
        Map<String, Object> permBody = new HashMap<>();
        permBody.put("permCode", "apply:controller");
        permBody.put("permName", "申请测试权限");
        String permResp = mockMvc.perform(post("/internal/permission/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(permBody)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Long applyPermId = ((Number) ((Map<String, Object>) objectMapper.readValue(permResp, Map.class)
                .get("data")).get("permId")).longValue();

        Map<String, Object> body = new HashMap<>();
        body.put("userId", testUserId);
        body.put("permId", applyPermId);

        mockMvc.perform(post("/internal/user-permission/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    // ============================ 自驱定时任务 ============================

    @Test
    @Order(30)
    void testExpireStaleAuths() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("days", 14);

        mockMvc.perform(post("/internal/scheduler/expire-stale-auths")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.affectedCount").exists());
    }

    @Test
    @Order(31)
    void testDeactivateInactiveUsers() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("days", 30);

        mockMvc.perform(post("/internal/scheduler/deactivate-inactive-users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.affectedCount").exists());
    }

    @Test
    @Order(32)
    void testPurgeDeregisteredUsers() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("days", 30);

        mockMvc.perform(post("/internal/scheduler/purge-deregistered-users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @Order(33)
    void testCleanExpiredCaches() throws Exception {
        mockMvc.perform(post("/internal/scheduler/clean-expired-caches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.note").exists());
    }

    @Test
    @Order(34)
    void testCleanExpiredData() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("days", 90);

        mockMvc.perform(post("/internal/scheduler/clean-expired-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.retentionDays").value(90));
    }

    // ============================ v0.4.0: 会话管理 ============================

    @Test
    @Order(35)
    void testRegisterSession() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", testUserId);
        body.put("sessionId", "ctrl-session-001");
        body.put("deviceType", "WEB");
        body.put("clientIp", "10.0.0.99");

        mockMvc.perform(post("/internal/user/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @Order(36)
    void testGetUserSessions() throws Exception {
        mockMvc.perform(get("/internal/user/{userId}/sessions", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sessions").isArray());
    }

    @Test
    @Order(37)
    void testTerminateSession() throws Exception {
        mockMvc.perform(delete("/internal/user/sessions/{sessionId}", "ctrl-session-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @Order(38)
    void testTerminateOtherSessions() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("currentSessionId", "current-keep-session");

        mockMvc.perform(delete("/internal/user/{userId}/sessions/others", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @Order(39)
    void testCleanStaleSessions() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("ttlMinutes", 30);

        mockMvc.perform(post("/internal/user/sessions/clean-stale")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    // ============================ v0.4.0: IP 突变 ============================

    @Test
    @Order(40)
    void testScanIpMutations() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("hours", 24);

        mockMvc.perform(post("/internal/user/login-history/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.scannedUsers").exists());
    }

    // ============================ 用户生命周期：注销 ============================

    @Test
    @Order(50)
    void testDeregister() throws Exception {
        mockMvc.perform(post("/internal/user/{userId}/deregister", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @Order(51)
    void testDeregisterNotFound() throws Exception {
        mockMvc.perform(post("/internal/user/{userId}/deregister", 99999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @Order(52)
    void testLogout() throws Exception {
        // testUserId 已被注销，使用之前的管理员创建的用户
        mockMvc.perform(post("/internal/user/{userId}/logout", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    // ============================ 管理员删除用户 ============================

    @Test
    @Order(53)
    void testDeleteUserNotFound() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 99999);

        mockMvc.perform(delete("/internal/user/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ============================ 分组和权限删除（级联） ============================

    @Test
    @Order(54)
    void testDeleteGroup() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("groupId", testGroupId);

        mockMvc.perform(delete("/internal/group/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @Order(55)
    void testDeletePermission() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("permId", testPermId);

        mockMvc.perform(delete("/internal/permission/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
