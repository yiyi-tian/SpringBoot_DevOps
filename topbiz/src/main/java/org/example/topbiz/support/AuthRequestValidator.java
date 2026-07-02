package org.example.topbiz.support;

import org.example.common.auth.AuthRequestPhase;
import org.example.common.auth.CredentialType;
import org.example.common.auth.CredentialValidator;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Auth request validation for unified register/login endpoints.
 */
public final class AuthRequestValidator {

    public static final String PHONE_SMS_NOT_CONNECTED =
            "未接入第三方短信平台，手机验证码仅支持格式校验，请使用邮箱注册/登录或手机号+密码";

    private AuthRequestValidator() {
    }

    public static Optional<String> validateUnifiedRegister(Map<String, Object> request) {
        Optional<CredentialType> typeOpt = CredentialValidator.inferCredentialType(request);
        if (typeOpt.isEmpty()) {
            return Optional.of("请提供 email / phone / credential / username");
        }
        CredentialType type = typeOpt.get();
        String credential = CredentialValidator.extractCredential(request);
        if (credential == null || credential.isBlank()) {
            return Optional.of("凭证不能为空");
        }
        Optional<String> formatErr = CredentialValidator.validate(type, credential);
        if (formatErr.isPresent()) {
            return formatErr;
        }

        AuthRequestPhase phase = CredentialValidator.resolvePhase(request);

        // 短信验证码未接入
        if (type == CredentialType.PHONE && phase == AuthRequestPhase.SEND_CODE) {
            return Optional.of(PHONE_SMS_NOT_CONNECTED);
        }
        if (type == CredentialType.PHONE && phase == AuthRequestPhase.CODE_AUTH) {
            return Optional.of(PHONE_SMS_NOT_CONNECTED);
        }

        // 验证码注册：不需要密码，只需 code
        if (phase == AuthRequestPhase.CODE_AUTH) {
            String code = String.valueOf(request.get("code")).trim();
            if (code.isEmpty()) {
                return Optional.of("验证码不能为空");
            }
            return Optional.empty();
        }

        // 密码注册：需要密码
        if (phase == AuthRequestPhase.PASSWORD_AUTH) {
            String password = String.valueOf(request.get("password")).trim();
            if (password.length() < 6) {
                return Optional.of("密码长度不能少于6位");
            }
            return Optional.empty();
        }

        // 发码：不需要额外校验
        if (phase == AuthRequestPhase.SEND_CODE) {
            return Optional.empty();
        }

        return Optional.of("请求参数异常，请检查 password 或 code");
    }

    public static Optional<String> validateUnifiedLogin(Map<String, Object> request) {
        Optional<CredentialType> typeOpt = CredentialValidator.inferCredentialType(request);
        if (typeOpt.isEmpty()) {
            return Optional.of("请提供 email / phone / credential / username");
        }
        CredentialType type = typeOpt.get();
        String credential = CredentialValidator.extractCredential(request);
        if (credential == null || credential.isBlank()) {
            return Optional.of("凭证不能为空");
        }
        Optional<String> formatErr = CredentialValidator.validate(type, credential);
        if (formatErr.isPresent()) {
            return formatErr;
        }

        AuthRequestPhase phase = CredentialValidator.resolvePhase(request);

        if (type == CredentialType.PHONE && (phase == AuthRequestPhase.SEND_CODE || phase == AuthRequestPhase.CODE_AUTH)) {
            return Optional.of(PHONE_SMS_NOT_CONNECTED);
        }

        // 验证码登录：只需 code
        if (phase == AuthRequestPhase.CODE_AUTH) {
            String code = String.valueOf(request.get("code")).trim();
            if (code.isEmpty()) {
                return Optional.of("验证码不能为空");
            }
            return Optional.empty();
        }

        // 密码登录：需要密码
        if (phase == AuthRequestPhase.PASSWORD_AUTH) {
            String password = String.valueOf(request.get("password")).trim();
            if (password.length() < 6) {
                return Optional.of("密码长度不能少于6位");
            }
            return Optional.empty();
        }

        // 发码：不需要额外校验
        if (phase == AuthRequestPhase.SEND_CODE) {
            return Optional.empty();
        }

        return Optional.of("请求参数异常，请检查 password 或 code");
    }

    public static int httpCodeForError(String message) {
        if (PHONE_SMS_NOT_CONNECTED.equals(message)) {
            return 501;
        }
        return 400;
    }

    public static Optional<String> validateEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.of("请输入邮箱");
        }
        return CredentialValidator.validate(CredentialType.EMAIL, email);
    }

    public static Optional<String> validatePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return Optional.of("请输入手机号");
        }
        return CredentialValidator.validate(CredentialType.PHONE, phone);
    }

    public static Map<String, Object> phoneCodeNotConnectedResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 501);
        result.put("message", PHONE_SMS_NOT_CONNECTED);
        return result;
    }

    public static Optional<String> validateResetPassword(Map<String, Object> request) {
        Optional<CredentialType> typeOpt = CredentialValidator.inferCredentialType(request);
        if (typeOpt.isEmpty()) {
            return Optional.of("请提供 email / phone / credential");
        }
        CredentialType type = typeOpt.get();
        String credential = CredentialValidator.extractCredential(request);
        if (credential == null || credential.isBlank()) {
            return Optional.of("请提供 credential / email / phone");
        }
        Optional<String> formatErr = CredentialValidator.validate(type, credential);
        if (formatErr.isPresent()) {
            return formatErr;
        }
        String code = request.get("code") != null ? String.valueOf(request.get("code")).trim() : "";
        if (code.isEmpty()) {
            return Optional.of("验证码不能为空");
        }
        String newPassword = request.get("newPassword") != null ? String.valueOf(request.get("newPassword")) : "";
        if (newPassword.length() < 6) {
            return Optional.of("新密码长度不能少于6位");
        }
        return Optional.empty();
    }

    public static Optional<String> validateBindCredential(Map<String, Object> request) {
        Optional<CredentialType> typeOpt = CredentialValidator.inferCredentialType(request);
        if (typeOpt.isEmpty()) {
            return Optional.of("credentialType 必填，且必须为 EMAIL / PHONE / USERNAME");
        }
        CredentialType type = typeOpt.get();
        String credential = CredentialValidator.extractCredential(request);
        if (credential == null || credential.isBlank()) {
            return Optional.of("请输入要绑定的手机号/邮箱/用户名");
        }
        return CredentialValidator.validate(type, credential);
    }
}