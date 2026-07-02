package org.example.topbiz.service;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.example.common.message.MessageConstants;
import org.example.common.auth.AuthRequestPhase;
import org.example.common.auth.CredentialType;
import org.example.common.auth.CredentialValidator;
import org.example.topbiz.exception.InternalServiceException;
import org.example.topbiz.feign.LogServiceClient;
import org.example.topbiz.feign.MessageServiceClient;
import org.example.topbiz.feign.UserServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 认证服务：注册、登录、验证码
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private MessageServiceClient messageServiceClient;

    @Autowired
    private LogServiceClient logServiceClient;

    @Autowired
    private WelcomeMessageService welcomeMessageService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 统一注册入口：发码 / 验证码注册 / 密码注册
     */
    public Map<String, Object> handleRegister(Map<String, Object> request, String clientIp, String userAgent) {
        CredentialType credentialType = CredentialValidator.inferCredentialType(request).orElse(null);
        if (credentialType == null) {
            return error(400, "无法识别凭证类型");
        }
        String credential = CredentialValidator.normalizeCredential(
                credentialType, CredentialValidator.extractCredential(request));
        AuthRequestPhase phase = CredentialValidator.resolvePhase(request);
        String deviceId = resolveDeviceId(request);

        return switch (phase) {
            case SEND_CODE -> sendRegisterCode(credentialType, credential);
            case CODE_AUTH -> completeRegister(credentialType, credential, null,
                    String.valueOf(request.get("code")).trim(), false, clientIp, userAgent, deviceId);
            case PASSWORD_WITH_CODE -> completeRegister(credentialType, credential,
                    String.valueOf(request.get("password")).trim(),
                    String.valueOf(request.get("code")).trim(), true, clientIp, userAgent, deviceId);
            case PASSWORD_AUTH -> completeRegister(credentialType, credential,
                    String.valueOf(request.get("password")).trim(), null, false, clientIp, userAgent, deviceId);
        };
    }

    /**
     * 统一登录入口：发码 / 验证码登录 / 密码登录
     */
    public Map<String, Object> handleLogin(Map<String, Object> request, String clientIp, String userAgent) {
        CredentialType credentialType = CredentialValidator.inferCredentialType(request).orElse(null);
        if (credentialType == null) {
            return error(400, "无法识别凭证类型");
        }
        String credential = CredentialValidator.normalizeCredential(
                credentialType, CredentialValidator.extractCredential(request));
        AuthRequestPhase phase = CredentialValidator.resolvePhase(request);
        String deviceId = resolveDeviceId(request);

        return switch (phase) {
            case SEND_CODE -> sendLoginCode(credentialType, credential);
            case CODE_AUTH -> completeLogin(credentialType, credential, null,
                    String.valueOf(request.get("code")).trim(), clientIp, userAgent, deviceId);
            case PASSWORD_AUTH -> completeLogin(credentialType, credential,
                    String.valueOf(request.get("password")).trim(), null, clientIp, userAgent, deviceId);
            case PASSWORD_WITH_CODE -> error(400, "登录不支持同时提交 password 与 code");
        };
    }

    private Map<String, Object> sendRegisterCode(CredentialType credentialType, String credential) {
        if (credentialType == CredentialType.EMAIL) {
            return sendRegisterEmailCode(credential);
        }
        return error(501, org.example.topbiz.support.AuthRequestValidator.PHONE_SMS_NOT_CONNECTED);
    }

    private Map<String, Object> sendLoginCode(CredentialType credentialType, String credential) {
        if (credentialType == CredentialType.EMAIL) {
            return sendLoginEmailCode(credential);
        }
        return error(501, org.example.topbiz.support.AuthRequestValidator.PHONE_SMS_NOT_CONNECTED);
    }

    private Map<String, Object> completeRegister(CredentialType credentialType, String credential,
                                                  String password, String code, boolean passwordWithCode,
                                                  String clientIp, String userAgent, String deviceId) {
        if (credentialType == CredentialType.PHONE && code != null && !code.isBlank()) {
            return error(501, org.example.topbiz.support.AuthRequestValidator.PHONE_SMS_NOT_CONNECTED);
        }

        boolean verifyCodeRequired = (code != null && !code.isBlank());
        if (verifyCodeRequired) {
            boolean valid = verifyCode(credentialType.name(), credential, MessageConstants.SCENE_REGISTER, code);
            if (!valid) {
                return error(400, "验证码错误或已过期");
            }
        }

        if (password == null || password.isBlank()) {
            return error(400, org.example.topbiz.support.AuthRequestValidator.REGISTER_CODE_NEEDS_PASSWORD);
        }

        Map<String, Object> userRequest = new HashMap<>();
        userRequest.put("credentialType", credentialType.name());
        userRequest.put("credential", credential);
<<<<<<< HEAD
        userRequest.put("password", code != null ? "" : password);
        userRequest.put("code", code);
=======
        userRequest.put("password", password.trim());
>>>>>>> develop2

        Map<String, Object> userResult = userServiceClient.register(userRequest);
        if (userResult == null || !"0".equals(String.valueOf(userResult.get("code")))) {
            return userResult;
        }

        Map<String, Object> userData = (Map<String, Object>) userResult.get("data");
        Long userId = Long.valueOf(String.valueOf(userData.get("userId")));

        welcomeMessageService.sendWelcomeMessage(userId);
        recordAudit(userId, "USER_REGISTER", String.valueOf(userId));
        establishLoginSession(userId, clientIp, userAgent, deviceId);
        return userResult;
    }

    private Map<String, Object> completeLogin(CredentialType credentialType, String credential,
                                             String password, String code,
                                             String clientIp, String userAgent, String deviceId) {
        if (credentialType == CredentialType.PHONE && code != null && !code.isBlank()) {
            return error(501, org.example.topbiz.support.AuthRequestValidator.PHONE_SMS_NOT_CONNECTED);
        }

        boolean codeVerified = false;
        if (code != null && !code.isBlank()) {
            boolean valid = verifyCode(credentialType.name(), credential, MessageConstants.SCENE_LOGIN, code);
            if (!valid) {
                return error(400, "验证码错误或已过期");
            }
            codeVerified = true;
        }

        Map<String, Object> userRequest = new HashMap<>();
        userRequest.put("credentialType", credentialType.name());
        userRequest.put("credential", credential);
        userRequest.put("password", password);
        userRequest.put("clientIp", clientIp);
        userRequest.put("userAgent", userAgent);
        if (codeVerified) {
            userRequest.put("codeVerified", true);
        }

        Map<String, Object> userResult = userServiceClient.login(userRequest);
        if (userResult == null || !"0".equals(String.valueOf(userResult.get("code")))) {
            return userResult;
        }

        Map<String, Object> userData = (Map<String, Object>) userResult.get("data");
        Long userId = Long.valueOf(String.valueOf(userData.get("userId")));

        establishLoginSession(userId, clientIp, userAgent, deviceId);
        recordAudit(userId, "USER_LOGIN", null);
        return userResult;
    }

    private void establishLoginSession(Long userId, String clientIp, String userAgent, String deviceId) {
        Subject subject = SecurityUtils.getSubject();
        subject.login(new UsernamePasswordToken(String.valueOf(userId), "NOT_USED"));
        subject.getSession().setAttribute("deviceId", deviceId);

        try {
            String sessionId = subject.getSession().getId().toString();
            String deviceType = detectDeviceType(userAgent);
            Map<String, Object> sessionRequest = new HashMap<>();
            sessionRequest.put("userId", userId);
            sessionRequest.put("sessionId", sessionId);
            sessionRequest.put("deviceId", deviceId);
            sessionRequest.put("deviceType", deviceType);
            sessionRequest.put("clientIp", clientIp);
            sessionRequest.put("userAgent", userAgent);
            userServiceClient.registerSession(sessionRequest);
        } catch (Exception e) {
            log.error("注册用户会话失败 userId={}: {}", userId, e.getMessage(), e);
        }
    }

    private Map<String, Object> sendRegisterEmailCode(String email) {
        email = CredentialValidator.normalizeEmail(email);
        Map<String, Object> msgRequest = new HashMap<>();
        msgRequest.put("email", email);
        msgRequest.put("scene", MessageConstants.SCENE_REGISTER);
        return messageServiceClient.sendEmailCode(msgRequest);
    }

    /**
     * 发送注册手机验证码 — 未接第三方短信
     */
    private Map<String, Object> sendRegisterPhoneCode(String phone) {
        return error(501, org.example.topbiz.support.AuthRequestValidator.PHONE_SMS_NOT_CONNECTED);
    }

    private Map<String, Object> sendLoginEmailCode(String email) {
        email = CredentialValidator.normalizeEmail(email);
        Map<String, Object> msgRequest = new HashMap<>();
        msgRequest.put("email", email);
        msgRequest.put("scene", MessageConstants.SCENE_LOGIN);
        return messageServiceClient.sendEmailCode(msgRequest);
    }

    private Map<String, Object> sendLoginPhoneCode(String phone) {
        return error(501, org.example.topbiz.support.AuthRequestValidator.PHONE_SMS_NOT_CONNECTED);
    }

    // ==================== 注销、登出、权限、分组 ====================

    /**
     * 注销账户（用户主动注销）
     */
    public Map<String, Object> deregister(Long userId) {
        Map<String, Object> result = userServiceClient.deregister(userId);

        if (result != null && "0".equals(String.valueOf(result.get("code")))) {
            try {
                String sessionId = SecurityUtils.getSubject().getSession().getId().toString();
                userServiceClient.terminateSession(sessionId);
            } catch (Exception e) {
                System.err.println("标记会话终止失败: " + e.getMessage());
            }

            try {
                SecurityUtils.getSubject().logout();
            } catch (Exception e) {
                System.err.println("Shiro 登出失败: " + e.getMessage());
            }
            recordAudit(userId, "USER_DEREGISTER", String.valueOf(userId));
        }

        return result;
    }

    /**
     * 登出（退出登录）v0.4.0: 标记会话为 TERMINATED
     */
    public Map<String, Object> logout(Long userId) {
        Map<String, Object> result = userServiceClient.logout(userId);

        // v0.4.0: 标记当前会话已终止
        try {
            String sessionId = SecurityUtils.getSubject().getSession().getId().toString();
            userServiceClient.terminateSession(sessionId);
        } catch (Exception e) {
            System.err.println("标记会话终止失败: " + e.getMessage());
        }

        try {
            SecurityUtils.getSubject().logout();
        } catch (Exception e) {
            System.err.println("Shiro 登出失败: " + e.getMessage());
        }

        recordAudit(userId, "USER_LOGOUT", null);
        return result;
    }

    /**
     * 获取当前用户的所有权限
     */
    public Map<String, Object> getPermissions(Long userId) {
        return userServiceClient.getPermissions(userId);
    }

    /**
     * 获取当前用户所属的所有组
     */
    public Map<String, Object> getGroups(Long userId) {
        return userServiceClient.getGroups(userId);
    }

    // ==================== 修改密码、基本信息、重置密码、账号绑定 ====================

    /**
     * 修改密码（已知旧密码）
     */
    public Map<String, Object> changePassword(Long userId, String oldPassword, String newPassword) {
        Map<String, Object> request = new HashMap<>();
        request.put("userId", userId);
        request.put("oldPassword", oldPassword);
        request.put("newPassword", newPassword);

        Map<String, Object> result = userServiceClient.changePassword(request);

        if (result != null && "0".equals(String.valueOf(result.get("code")))) {
            recordAudit(userId, "USER_CHANGE_PASSWORD", String.valueOf(userId));
        }

        return result;
    }

    /**
     * 修改个人基本信息
     */
    public Map<String, Object> updateProfile(Long userId, Map<String, Object> profileRequest) {
        profileRequest.put("userId", userId);
        Map<String, Object> result = userServiceClient.updateProfile(profileRequest);

        if (result != null && "0".equals(String.valueOf(result.get("code")))) {
            recordAudit(userId, "USER_UPDATE_PROFILE", String.valueOf(userId));
        }

        return result;
    }

    /**
     * 查询当前用户完整信息
     */
    public Map<String, Object> getProfile(Long userId) {
        return userServiceClient.getUserProfile(userId);
    }

    /**
     * 发送重置密码邮箱验证码
     */
    public Map<String, Object> sendResetPasswordEmailCode(String email) {
        email = CredentialValidator.normalizeEmail(email);
        Map<String, Object> msgRequest = new HashMap<>();
        msgRequest.put("email", email);
        msgRequest.put("scene", MessageConstants.SCENE_PASSWORD_RESET);
        return messageServiceClient.sendEmailCode(msgRequest);
    }

    public Map<String, Object> sendResetPasswordPhoneCode(String phone) {
        return error(501, org.example.topbiz.support.AuthRequestValidator.PHONE_SMS_NOT_CONNECTED);
    }

    /**
     * 重置密码（忘记密码 — 验证码校验后调用）
     */
    public Map<String, Object> resetPassword(Map<String, Object> request, String newPassword, String code) {
        CredentialType credentialType = CredentialValidator.inferCredentialType(request).orElse(null);
        if (credentialType == null) {
            return error(400, "无法识别凭证类型");
        }
        String credential = CredentialValidator.normalizeCredential(
                credentialType, CredentialValidator.extractCredential(request));

        if (credentialType == CredentialType.PHONE) {
            return error(501, org.example.topbiz.support.AuthRequestValidator.PHONE_SMS_NOT_CONNECTED);
        }

        if (code == null || code.isEmpty()) {
            return error(400, "验证码不能为空");
        }

        boolean valid = verifyCode(credentialType.name(), credential, MessageConstants.SCENE_PASSWORD_RESET, code);
        if (!valid) {
            return error(400, "验证码错误或已过期");
        }

        Map<String, Object> downstream = new HashMap<>();
        downstream.put("credentialType", credentialType.name());
        downstream.put("credential", credential);
        downstream.put("newPassword", newPassword);

        Map<String, Object> result = userServiceClient.resetPassword(downstream);

        if (result != null && "0".equals(String.valueOf(result.get("code")))) {
            recordAudit(null, "USER_RESET_PASSWORD", credentialType.name() + ":" + credential);
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            if (data != null && data.get("userId") != null) {
                try {
                    Long resetUserId = Long.valueOf(String.valueOf(data.get("userId")));
                    Long currentUserId = null;
                    try {
                        Object principal = SecurityUtils.getSubject().getPrincipal();
                        if (principal != null) {
                            currentUserId = Long.valueOf(String.valueOf(principal));
                        }
                    } catch (Exception ignored) {
                        // not logged in
                    }
                    if (currentUserId != null && currentUserId.equals(resetUserId)) {
                        try {
                            String sessionId = SecurityUtils.getSubject().getSession().getId().toString();
                            userServiceClient.terminateSession(sessionId);
                        } catch (Exception e) {
                            log.warn("重置密码后终止当前会话失败: {}", e.getMessage());
                        }
                        SecurityUtils.getSubject().logout();
                    }
                } catch (Exception e) {
                    log.warn("重置密码后会话清理失败: {}", e.getMessage());
                }
            }
        }

<<<<<<< HEAD
        // 调用 user-service 登录
        Map<String, Object> userRequest = new HashMap<>();
        userRequest.put("credentialType", credentialType);
        userRequest.put("credential", credential);
        userRequest.put("password", password);
        userRequest.put("code", code);
=======
        return result;
    }
>>>>>>> develop2

    public Map<String, Object> bindCredential(Long userId, Map<String, Object> request, String password) {
        CredentialType credentialType = CredentialValidator.inferCredentialType(request).orElse(null);
        if (credentialType == null) {
            return error(400, "无法识别凭证类型");
        }
        String credential = CredentialValidator.normalizeCredential(
                credentialType, CredentialValidator.extractCredential(request));
        Map<String, Object> downstream = new HashMap<>();
        downstream.put("userId", userId);
        downstream.put("credentialType", credentialType.name());
        downstream.put("credential", credential);
        downstream.put("password", password);

        Map<String, Object> result = userServiceClient.bindCredential(downstream);

        if (result != null && "0".equals(String.valueOf(result.get("code")))) {
            recordAudit(userId, "USER_BIND_CREDENTIAL", credentialType.name() + ":" + credential);
        }

        return result;
    }

    /**
     * 申请权限（1.1.11）
     */
    public Map<String, Object> applyForPermission(Long userId, Long permId) {
        Map<String, Object> request = new HashMap<>();
        request.put("userId", userId);
        request.put("permId", permId);

        Map<String, Object> result = userServiceClient.applyUserPermission(request);

        if (result != null && "0".equals(String.valueOf(result.get("code")))) {
            recordAudit(userId, "USER_PERMISSION_APPLY", "permId:" + permId);
        }

        return result;
    }

    /**
     * 获取权限目录（可申请权限列表，只读）
     */
    public Map<String, Object> searchPermissionsCatalog(Map<String, Object> params) {
        return userServiceClient.searchPermissions(params);
    }

    /**
     * 当前用户的权限申请记录（PENDING / REJECTED）
     */
    public Map<String, Object> getPermissionApplications(Long userId) {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        Map<String, Object> result = userServiceClient.searchUserPermissions(params);
        if (result == null || !"0".equals(String.valueOf(result.get("code")))) {
            return result;
        }
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        if (data == null) {
            return result;
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> all = (List<Map<String, Object>>) data.get("list");
        List<Map<String, Object>> applications = new ArrayList<>();
        if (all != null) {
            for (Map<String, Object> item : all) {
                String status = String.valueOf(item.get("status"));
                if ("PENDING".equals(status) || "REJECTED".equals(status)) {
                    applications.add(item);
                }
            }
        }
        data.put("list", applications);
        data.put("count", applications.size());
        return result;
    }

    // ==================== v0.4.0: 多端会话管理 ====================

    /**
     * 获取当前用户的所有活跃会话列表
     */
    public Map<String, Object> getSessions(Long userId) {
        Map<String, Object> result = userServiceClient.getUserSessions(userId);

        if (result != null && "0".equals(String.valueOf(result.get("code")))) {
            try {
                String currentSessionId = SecurityUtils.getSubject().getSession().getId().toString();
                String currentDeviceId = (String) SecurityUtils.getSubject().getSession().getAttribute("deviceId");
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                if (data == null) {
                    data = new HashMap<>();
                    result.put("data", data);
                }
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> sessions = (List<Map<String, Object>>) data.get("sessions");

                if (sessions == null || sessions.isEmpty()) {
                    String deviceId = currentDeviceId != null ? currentDeviceId : resolveDeviceId(Map.of());
                    Map<String, Object> sessionRequest = new HashMap<>();
                    sessionRequest.put("userId", userId);
                    sessionRequest.put("sessionId", currentSessionId);
                    sessionRequest.put("deviceId", deviceId);
                    sessionRequest.put("deviceType", "WEB");
                    userServiceClient.registerSession(sessionRequest);
                    if (currentDeviceId == null) {
                        SecurityUtils.getSubject().getSession().setAttribute("deviceId", deviceId);
                    }
                    result = userServiceClient.getUserSessions(userId);
                    data = (Map<String, Object>) result.get("data");
                    if (data != null) {
                        sessions = (List<Map<String, Object>>) data.get("sessions");
                    }
                }

                if (sessions == null || sessions.isEmpty()) {
                    sessions = new ArrayList<>();
                    Map<String, Object> current = new LinkedHashMap<>();
                    current.put("sessionId", currentSessionId);
                    current.put("deviceId", currentDeviceId);
                    current.put("deviceType", "WEB");
                    current.put("isCurrent", true);
                    sessions.add(current);
                    data.put("sessions", sessions);
                    data.put("count", 1);
                } else {
                    for (Map<String, Object> s : sessions) {
                        boolean bySession = currentSessionId.equals(s.get("sessionId"));
                        boolean byDevice = currentDeviceId != null
                                && currentDeviceId.equals(s.get("deviceId"));
                        s.put("isCurrent", bySession || byDevice);
                    }
                }
            } catch (Exception e) {
                log.warn("标记或补全会话失败 userId={}: {}", userId, e.getMessage());
            }
        }

        return result;
    }

    /**
     * 强制登出指定设备
     */
    public Map<String, Object> forceLogoutDevice(Long userId, String deviceId) {
        Map<String, Object> terminateResult = userServiceClient.terminateDevice(userId, deviceId);
        if (terminateResult == null || !"0".equals(String.valueOf(terminateResult.get("code")))) {
            return terminateResult;
        }
        Map<String, Object> data = (Map<String, Object>) terminateResult.get("data");
        String sessionId = data != null ? (String) data.get("sessionId") : null;
        if (sessionId == null) {
            Map<String, Object> ok = new HashMap<>();
            ok.put("code", 0);
            ok.put("message", "ok");
            return ok;
        }
        return invalidateShiroSession(userId, sessionId, "SESSION_TERMINATED", "deviceId:" + deviceId);
    }

    /**
     * 强制登出指定会话
     */
    public Map<String, Object> forceLogoutSession(Long userId, String sessionId) {
        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> terminateResult = userServiceClient.terminateSession(sessionId);
            if (terminateResult == null || !"0".equals(String.valueOf(terminateResult.get("code")))) {
                return terminateResult;
            }
            return invalidateShiroSession(userId, sessionId, "SESSION_TERMINATED", "sessionId:" + sessionId);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "强制登出失败: " + e.getMessage());
            return result;
        }
    }

    private Map<String, Object> invalidateShiroSession(Long userId, String sessionId, String auditOp, String auditDetail) {
        Map<String, Object> result = new HashMap<>();
        try {
            String currentSessionId = null;
            try {
                currentSessionId = SecurityUtils.getSubject().getSession().getId().toString();
            } catch (Exception ignored) {
                // 可能当前没有 Session
            }

            if (sessionId.equals(currentSessionId)) {
                SecurityUtils.getSubject().logout();
            } else {
                try {
                    redisTemplate.delete("shiro:session:sessions:" + sessionId);
                    redisTemplate.delete("shiro:session:sessions:expires:" + sessionId);
                } catch (Exception e) {
                    System.err.println("删除 Redis 会话 key 失败: " + e.getMessage());
                }
            }

            recordAudit(userId, auditOp, auditDetail);
            result.put("code", 0);
            result.put("message", "ok");
            return result;
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "强制登出失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 强制登出当前用户的所有其他会话
     */
    public Map<String, Object> forceLogoutOtherSessions(Long userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            String currentSessionId = SecurityUtils.getSubject().getSession().getId().toString();
            String currentDeviceId = (String) SecurityUtils.getSubject().getSession().getAttribute("deviceId");

            // 1. 查询所有其他活跃会话
            Map<String, Object> sessionsResult = userServiceClient.getUserSessions(userId);
            if (sessionsResult != null && "0".equals(String.valueOf(sessionsResult.get("code")))) {
                Map<String, Object> data = (Map<String, Object>) sessionsResult.get("data");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> sessions = (List<Map<String, Object>>) data.get("sessions");
                if (sessions != null) {
                    for (Map<String, Object> s : sessions) {
                        String sid = (String) s.get("sessionId");
                        if (!currentSessionId.equals(sid)) {
                            // 删除 Redis key
                            try {
                                redisTemplate.delete("shiro:session:sessions:" + sid);
                                redisTemplate.delete("shiro:session:sessions:expires:" + sid);
                            } catch (Exception ignored) {
                                // Redis 删除失败不阻塞
                            }
                        }
                    }
                }
            }

            // 2. 在 MySQL 中批量标记
            Map<String, Object> termRequest = new HashMap<>();
            termRequest.put("currentSessionId", currentSessionId);
            termRequest.put("currentDeviceId", currentDeviceId);
            Map<String, Object> termResult = userServiceClient.terminateOtherSessions(userId, termRequest);

            if (termResult != null && "0".equals(String.valueOf(termResult.get("code")))) {
                Map<String, Object> termData = (Map<String, Object>) termResult.get("data");
                recordAudit(userId, "SESSION_TERMINATED_ALL",
                        "terminatedCount=" + termData.get("terminatedCount"));
            }

            result.put("code", 0);
            result.put("message", "ok");
            if (termResult != null) {
                result.put("data", termResult.get("data"));
            }
            return result;

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "强制登出其他会话失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 根据 User-Agent 检测设备类型
     */
    private String detectDeviceType(String userAgent) {
        if (userAgent == null) return "UNKNOWN";
        String ua = userAgent.toLowerCase();
        if (ua.contains("android")) return "ANDROID";
        if (ua.contains("iphone") || ua.contains("ipad")) return "IOS";
        if (ua.contains("windows nt") || ua.contains("macintosh") || ua.contains("linux")) return "DESKTOP";
        if (ua.contains("mozilla") || ua.contains("chrome") || ua.contains("safari")) return "WEB";
        return "UNKNOWN";
    }

    private String resolveDeviceId(Map<String, Object> request) {
        Object raw = request.get("deviceId");
        if (raw != null && !String.valueOf(raw).isBlank()) {
            return String.valueOf(raw).trim();
        }
        String legacy = "legacy-" + UUID.randomUUID();
        log.warn("login/register 未携带 deviceId，已生成临时设备号: {}", legacy);
        return legacy;
    }

    // ==================== 私有工具方法 ====================

    /**
     * 校验验证码
     */
    private boolean verifyCode(String credentialType, String target, String scene, String code) {
        Map<String, Object> request = new HashMap<>();
        request.put("credentialType", credentialType);
        request.put("target", target);
        request.put("scene", scene);
        request.put("code", code);
        Map<String, Object> result;
        try {
            result = messageServiceClient.verifyCode(request);
        } catch (WebClientResponseException | WebClientRequestException e) {
            throw new InternalServiceException(503, "验证码服务暂不可用");
        }
        if (result == null) {
            throw new InternalServiceException(503, "验证码服务无响应");
        }
        return "0".equals(String.valueOf(result.get("code")));
    }

    /**
     * 自动识别凭证类型（兼容旧客户端，新接口应显式传 credentialType）
     */
    public String detectCredentialType(String credential) {
        Map<String, Object> req = new HashMap<>();
        req.put("credential", credential);
        return CredentialValidator.inferCredentialType(req)
                .map(CredentialType::name)
                .orElse(CredentialType.USERNAME.name());
    }

    /**
     * 提取凭证字段（兼容 phone/email/username）
     */
    public String extractCredential(Map<String, Object> request) {
        return CredentialValidator.extractCredential(request);
    }

    private void compensateRegister(Long userId) {
        try {
            Map<String, Object> req = new HashMap<>();
            req.put("userId", userId);
            userServiceClient.compensateRegister(req);
        } catch (Exception e) {
            System.err.println("注册补偿回滚失败 userId=" + userId + ": " + e.getMessage());
        }
    }

    private Map<String, Object> error(int code, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        return result;
    }

    /**
     * 记录审计日志
     */
    private void recordAudit(Long userId, String operation, String targetId) {
        try {
            Map<String, Object> logRequest = new HashMap<>();
            logRequest.put("trace_id", MDC.get("traceId"));
            logRequest.put("user_id", userId);
            logRequest.put("operation", operation);
            logRequest.put("success", true);
            logRequest.put("target_id", targetId);
            logServiceClient.recordAudit(logRequest);
        } catch (Exception e) {
            System.err.println("记录审计日志失败: " + e.getMessage());
        }
    }
}