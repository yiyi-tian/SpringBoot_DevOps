package org.example.topbiz.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.common.Result;
import org.example.topbiz.service.AuthService;
import org.example.topbiz.support.AuthRequestValidator;
import org.example.topbiz.support.SecurityUtilsHelper;
import org.example.topbiz.support.ServiceResultMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
public class AuthController {

    @Autowired
    private AuthService authService;

    // ==================== 注册 / 登录（统一入口） ====================

    @PostMapping("/api/v1/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, Object> request,
                                                HttpServletRequest httpRequest) {
        Optional<String> validationError = AuthRequestValidator.validateUnifiedRegister(request);
        if (validationError.isPresent()) {
            return Result.error(AuthRequestValidator.httpCodeForError(validationError.get()), validationError.get());
        }
        String clientIp = resolveClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ServiceResultMapper.toResult(authService.handleRegister(request, clientIp, userAgent));
    }

    @PostMapping("/api/v1/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, Object> request,
                                              HttpServletRequest httpRequest) {
        Optional<String> validationError = AuthRequestValidator.validateUnifiedLogin(request);
        if (validationError.isPresent()) {
            return Result.error(AuthRequestValidator.httpCodeForError(validationError.get()), validationError.get());
        }
        String clientIp = resolveClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ServiceResultMapper.toResult(authService.handleLogin(request, clientIp, userAgent));
    }

    // ==================== 注销、登出、权限、分组 ====================

    @PostMapping("/api/v1/deregister")
    public Result<Map<String, Object>> deregister() {
        Long userId = SecurityUtilsHelper.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期，请重新登录");
        }
        Map<String, Object> result = authService.deregister(userId);
        return ServiceResultMapper.toResult(result);
    }

    @PostMapping("/api/v1/logout")
    public Result<Map<String, Object>> logout() {
        Long userId = SecurityUtilsHelper.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期，请重新登录");
        }
        Map<String, Object> result = authService.logout(userId);
        return ServiceResultMapper.toResult(result);
    }

    @GetMapping("/api/v1/permissions")
    public Result<Map<String, Object>> getPermissions() {
        Long userId = SecurityUtilsHelper.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期，请重新登录");
        }
        Map<String, Object> result = authService.getPermissions(userId);
        return ServiceResultMapper.toResult(result);
    }

    @GetMapping("/api/v1/permissions/catalog")
    public Result<Map<String, Object>> searchPermissionsCatalog(@RequestParam Map<String, Object> params) {
        Long userId = SecurityUtilsHelper.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期，请重新登录");
        }
        Map<String, Object> result = authService.searchPermissionsCatalog(params);
        return ServiceResultMapper.toResult(result);
    }

    @GetMapping("/api/v1/permissions/applications")
    public Result<Map<String, Object>> getPermissionApplications() {
        Long userId = SecurityUtilsHelper.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期，请重新登录");
        }
        Map<String, Object> result = authService.getPermissionApplications(userId);
        return ServiceResultMapper.toResult(result);
    }

    @GetMapping("/api/v1/groups")
    public Result<Map<String, Object>> getGroups() {
        Long userId = SecurityUtilsHelper.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期，请重新登录");
        }
        Map<String, Object> result = authService.getGroups(userId);
        return ServiceResultMapper.toResult(result);
    }

    // ==================== 修改密码、基本信息、重置密码、账号绑定 ====================

    @PutMapping("/api/v1/password")
    public Result<Map<String, Object>> changePassword(@RequestBody Map<String, Object> request) {
        Long userId = SecurityUtilsHelper.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期，请重新登录");
        }
        String oldPassword = (String) request.get("oldPassword");
        String newPassword = (String) request.get("newPassword");

        if (oldPassword == null || oldPassword.isEmpty()) {
            return Result.error(400, "旧密码不能为空");
        }
        if (newPassword == null || newPassword.length() < 6) {
            return Result.error(400, "新密码长度不能少于6位");
        }

        Map<String, Object> result = authService.changePassword(userId, oldPassword, newPassword);
        return ServiceResultMapper.toResult(result);
    }

    @PatchMapping("/api/v1/profile")
    public Result<Map<String, Object>> updateProfile(@RequestBody Map<String, Object> request) {
        Long userId = SecurityUtilsHelper.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期，请重新登录");
        }
        Object displayName = request.get("displayName");
        if (displayName != null && String.valueOf(displayName).length() > 64) {
            return Result.error(400, "displayName 长度不能超过 64");
        }
        Map<String, Object> result = authService.updateProfile(userId, request);
        return ServiceResultMapper.toResult(result);
    }

    @GetMapping("/api/v1/profile")
    public Result<Map<String, Object>> getProfile() {
        Long userId = SecurityUtilsHelper.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期，请重新登录");
        }
        Map<String, Object> result = authService.getProfile(userId);
        return ServiceResultMapper.toResult(result);
    }

    @PostMapping("/api/v1/password/reset/email_code")
    public Result<Void> sendResetPasswordEmailCode(@RequestBody Map<String, String> request) {
        Optional<String> err = AuthRequestValidator.validateEmail(request.get("email"));
        if (err.isPresent()) {
            return Result.error(400, err.get());
        }
        return ServiceResultMapper.toResult(authService.sendResetPasswordEmailCode(request.get("email")));
    }

    @PostMapping("/api/v1/password/reset/phone_code")
    public Result<Void> sendResetPasswordPhoneCode(@RequestBody Map<String, String> request) {
        Optional<String> err = AuthRequestValidator.validatePhone(request.get("phone"));
        if (err.isPresent()) {
            return Result.error(400, err.get());
        }
        return ServiceResultMapper.toResult(authService.sendResetPasswordPhoneCode(request.get("phone")));
    }

    @PostMapping("/api/v1/password/reset")
    public Result<Map<String, Object>> resetPassword(@RequestBody Map<String, Object> request) {
        Optional<String> err = AuthRequestValidator.validateResetPassword(request);
        if (err.isPresent()) {
            return Result.error(400, err.get());
        }
        String newPassword = String.valueOf(request.get("newPassword"));
        String code = String.valueOf(request.get("code"));
        Map<String, Object> result = authService.resetPassword(request, newPassword, code);
        return ServiceResultMapper.toResult(result);
    }

    @PostMapping("/api/v1/account/bind")
    public Result<Map<String, Object>> bindCredential(@RequestBody Map<String, Object> request) {
        Long userId = SecurityUtilsHelper.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期，请重新登录");
        }

        Optional<String> err = AuthRequestValidator.validateBindCredential(request);
        if (err.isPresent()) {
            return Result.error(400, err.get());
        }

        String password = (String) request.get("password");
        Map<String, Object> result = authService.bindCredential(userId, request, password);
        return ServiceResultMapper.toResult(result);
    }

    // ==================== 权限申请 ====================

    @PostMapping("/api/v1/permissions/apply")
    public Result<Map<String, Object>> applyForPermission(@RequestBody Map<String, Object> request) {
        Long userId = SecurityUtilsHelper.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期，请重新登录");
        }

        Object permIdObj = request.get("permId");
        if (permIdObj == null) {
            return Result.error(400, "permId 必填");
        }
        long permId = Long.parseLong(String.valueOf(permIdObj));
        if (permId <= 0) {
            return Result.error(400, "permId 必须大于 0");
        }

        Map<String, Object> result = authService.applyForPermission(userId, permId);
        return ServiceResultMapper.toResult(result);
    }

    // ==================== 多端会话管理 ====================

    @GetMapping("/api/v1/sessions")
    public Result<Map<String, Object>> getSessions() {
        Long userId = SecurityUtilsHelper.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期，请重新登录");
        }
        Map<String, Object> result = authService.getSessions(userId);
        return ServiceResultMapper.toResult(result);
    }

    @DeleteMapping("/api/v1/sessions/{sessionId}")
    public Result<Map<String, Object>> forceLogoutSession(@PathVariable String sessionId) {
        Long userId = SecurityUtilsHelper.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期，请重新登录");
        }
        if (sessionId == null || sessionId.isBlank()) {
            return Result.error(400, "sessionId 不能为空");
        }
        Map<String, Object> result = authService.forceLogoutSession(userId, sessionId);
        return ServiceResultMapper.toResult(result);
    }

    @DeleteMapping("/api/v1/devices/{deviceId}")
    public Result<Map<String, Object>> forceLogoutDevice(@PathVariable String deviceId) {
        Long userId = SecurityUtilsHelper.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期，请重新登录");
        }
        if (deviceId == null || deviceId.isBlank()) {
            return Result.error(400, "deviceId 不能为空");
        }
        Map<String, Object> result = authService.forceLogoutDevice(userId, deviceId);
        return ServiceResultMapper.toResult(result);
    }

    @DeleteMapping("/api/v1/sessions")
    public Result<Map<String, Object>> forceLogoutOtherSessions(
            @RequestParam(value = "scope", required = false) String scope) {
        Long userId = SecurityUtilsHelper.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期，请重新登录");
        }
        if (!"others".equals(scope)) {
            return Result.error(400, "scope 参数必须为 others，或不传 sessionId 时使用 DELETE /api/v1/sessions/{sessionId}");
        }
        Map<String, Object> result = authService.forceLogoutOtherSessions(userId);
        return ServiceResultMapper.toResult(result);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
