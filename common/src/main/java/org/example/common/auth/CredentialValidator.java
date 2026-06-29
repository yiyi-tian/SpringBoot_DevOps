package org.example.common.auth;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Strict credential format validation for auth flows.
 */
public final class CredentialValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?"
                    + "(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)+$");

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{3,31}$");

    private CredentialValidator() {
    }

    public static String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static Optional<String> validate(CredentialType type, String credential) {
        if (type == null) {
            return Optional.of("credentialType 必填，且必须为 EMAIL / PHONE / USERNAME");
        }
        if (credential == null || credential.isBlank()) {
            return Optional.of("凭证不能为空");
        }
        String value = credential.trim();
        if (type == CredentialType.EMAIL) {
            String normalized = normalizeEmail(value);
            if (!EMAIL_PATTERN.matcher(normalized).matches()) {
                return Optional.of("邮箱格式无效");
            }
            return Optional.empty();
        }
        if (type == CredentialType.PHONE) {
            if (!PHONE_PATTERN.matcher(value).matches()) {
                return Optional.of("手机号格式无效，须为大陆 11 位手机号");
            }
            return Optional.empty();
        }
        if (type == CredentialType.USERNAME) {
            if (!USERNAME_PATTERN.matcher(value).matches()) {
                return Optional.of("用户名须以字母开头，4-32 位字母数字下划线");
            }
            return Optional.empty();
        }
        return Optional.of("不支持的 credentialType");
    }

    public static Optional<String> validate(String credentialType, String credential) {
        return validate(CredentialType.parse(credentialType), credential);
    }

    /**
     * Normalize credential for storage/lookup (email lowercased).
     */
    public static String normalizeCredential(CredentialType type, String credential) {
        if (credential == null) {
            return null;
        }
        if (type == CredentialType.EMAIL) {
            return normalizeEmail(credential);
        }
        return credential.trim();
    }

    /**
     * Extract credential from request map (credential / email / phone / username).
     */
    public static String extractCredential(Map<String, Object> request) {
        if (request == null) {
            return null;
        }
        Object credential = request.get("credential");
        if (credential != null && !String.valueOf(credential).isBlank()) {
            return String.valueOf(credential).trim();
        }
        Object email = request.get("email");
        if (email != null && !String.valueOf(email).isBlank()) {
            return String.valueOf(email).trim();
        }
        Object phone = request.get("phone");
        if (phone != null && !String.valueOf(phone).isBlank()) {
            return String.valueOf(phone).trim();
        }
        Object username = request.get("username");
        if (username != null && !String.valueOf(username).isBlank()) {
            return String.valueOf(username).trim();
        }
        return null;
    }

    public static Optional<AuthMethod> resolveAuthMethod(Map<String, Object> request) {
        if (request == null) {
            return Optional.empty();
        }
        String code = request.get("code") != null ? String.valueOf(request.get("code")).trim() : "";
        String password = request.get("password") != null ? String.valueOf(request.get("password")).trim() : "";
        boolean hasCode = !code.isEmpty();
        boolean hasPassword = !password.isEmpty();
        if (hasCode && hasPassword) {
            return Optional.empty();
        }
        if (hasCode) {
            return Optional.of(AuthMethod.CODE);
        }
        if (hasPassword) {
            return Optional.of(AuthMethod.PASSWORD);
        }
        return Optional.empty();
    }

    public static Optional<String> validateAuthMethodExclusivity(Map<String, Object> request) {
        Optional<AuthMethod> method = resolveAuthMethod(request);
        if (method.isEmpty()) {
            String code = request.get("code") != null ? String.valueOf(request.get("code")).trim() : "";
            String password = request.get("password") != null ? String.valueOf(request.get("password")).trim() : "";
            if (!code.isEmpty() && !password.isEmpty()) {
                return Optional.of("password 与 code 不能同时填写，请选择一种登录/注册方式");
            }
            return Optional.of("请提供 password（密码方式）或 code（验证码方式）");
        }
        return Optional.empty();
    }

    /**
     * Infer credential type from request fields (email / phone / credential / username).
     * Explicit credentialType wins when valid.
     */
    public static Optional<CredentialType> inferCredentialType(Map<String, Object> request) {
        if (request == null) {
            return Optional.empty();
        }
        Object explicit = request.get("credentialType");
        if (explicit != null && !String.valueOf(explicit).isBlank()) {
            CredentialType parsed = CredentialType.parse(String.valueOf(explicit));
            if (parsed != null) {
                return Optional.of(parsed);
            }
            return Optional.empty();
        }
        if (hasNonBlank(request, "email")) {
            return Optional.of(CredentialType.EMAIL);
        }
        if (hasNonBlank(request, "phone")) {
            return Optional.of(CredentialType.PHONE);
        }
        String credential = extractCredential(request);
        if (credential == null || credential.isBlank()) {
            return Optional.empty();
        }
        if (EMAIL_PATTERN.matcher(normalizeEmail(credential)).matches()) {
            return Optional.of(CredentialType.EMAIL);
        }
        if (PHONE_PATTERN.matcher(credential.trim()).matches()) {
            return Optional.of(CredentialType.PHONE);
        }
        return Optional.of(CredentialType.USERNAME);
    }

    /**
     * Resolve unified register/login phase from request body.
     */
    public static AuthRequestPhase resolvePhase(Map<String, Object> request) {
        String code = request.get("code") != null ? String.valueOf(request.get("code")).trim() : "";
        String password = request.get("password") != null ? String.valueOf(request.get("password")).trim() : "";
        boolean hasCode = !code.isEmpty();
        boolean hasPassword = !password.isEmpty();
        if (!hasCode && !hasPassword) {
            return AuthRequestPhase.SEND_CODE;
        }
        if (hasCode && hasPassword) {
            return AuthRequestPhase.PASSWORD_WITH_CODE;
        }
        if (hasCode) {
            return AuthRequestPhase.CODE_AUTH;
        }
        return AuthRequestPhase.PASSWORD_AUTH;
    }

    private static boolean hasNonBlank(Map<String, Object> request, String key) {
        Object val = request.get(key);
        return val != null && !String.valueOf(val).isBlank();
    }
}
