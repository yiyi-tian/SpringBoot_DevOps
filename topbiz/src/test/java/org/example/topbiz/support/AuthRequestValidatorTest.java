package org.example.topbiz.support;

import org.example.common.auth.AuthRequestPhase;
import org.example.common.auth.CredentialValidator;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuthRequestValidatorTest {

    @Test
    void sendCodePhaseEmailOnly() {
        Map<String, Object> req = new HashMap<>();
        req.put("email", "user@example.com");
        assertEquals(AuthRequestPhase.SEND_CODE, CredentialValidator.resolvePhase(req));
        assertTrue(AuthRequestValidator.validateUnifiedRegister(req).isEmpty());
        assertTrue(AuthRequestValidator.validateUnifiedLogin(req).isEmpty());
    }

    @Test
    void emailPasswordRegisterWithoutCodeValid() {
        Map<String, Object> req = new HashMap<>();
        req.put("email", "user@example.com");
        req.put("password", "secret123");
        assertEquals(AuthRequestPhase.PASSWORD_AUTH, CredentialValidator.resolvePhase(req));
        assertTrue(AuthRequestValidator.validateUnifiedRegister(req).isEmpty());
    }

    @Test
    void emailPasswordWithCodeRegisterRejected() {
        // PASSWORD_WITH_CODE 已移除，同时提交 password 和 code 应被拒绝
        Map<String, Object> req = new HashMap<>();
        req.put("email", "user@example.com");
        req.put("password", "secret123");
        req.put("code", "123456");
        // 由于同时存在 password 和 code，resolvePhase 可能返回 PASSWORD_WITH_CODE
        // 但 validator 现在会拒绝这种组合
        Optional<String> err = AuthRequestValidator.validateUnifiedRegister(req);
        assertTrue(err.isPresent());
        assertEquals("请求参数异常，请检查 password 或 code", err.get());
    }

    @Test
    void emailCodeOnlyRegisterValid() {
        // 验证码注册现在是被支持的
        Map<String, Object> req = new HashMap<>();
        req.put("email", "user@example.com");
        req.put("code", "123456");
        assertEquals(AuthRequestPhase.CODE_AUTH, CredentialValidator.resolvePhase(req));
        assertTrue(AuthRequestValidator.validateUnifiedRegister(req).isEmpty());
    }

    @Test
    void emailCodeOnlyRegisterWithEmptyCodeRejected() {
        Map<String, Object> req = new HashMap<>();
        req.put("email", "user@example.com");
        req.put("code", "");
        Optional<String> err = AuthRequestValidator.validateUnifiedRegister(req);
        assertTrue(err.isPresent());
        assertEquals("验证码不能为空", err.get());
    }

    @Test
    void phoneSendCodeReturns501Message() {
        Map<String, Object> req = new HashMap<>();
        req.put("phone", "13800138000");
        Optional<String> err = AuthRequestValidator.validateUnifiedRegister(req);
        assertTrue(err.isPresent());
        assertEquals(AuthRequestValidator.PHONE_SMS_NOT_CONNECTED, err.get());
        assertEquals(501, AuthRequestValidator.httpCodeForError(err.get()));
    }

    @Test
    void phoneCodeRegisterReturns501Message() {
        Map<String, Object> req = new HashMap<>();
        req.put("phone", "13800138000");
        req.put("code", "123456");
        Optional<String> err = AuthRequestValidator.validateUnifiedRegister(req);
        assertTrue(err.isPresent());
        assertEquals(AuthRequestValidator.PHONE_SMS_NOT_CONNECTED, err.get());
    }

    @Test
    void loginRejectsPasswordAndCodeTogether() {
        Map<String, Object> req = new HashMap<>();
        req.put("email", "user@example.com");
        req.put("password", "secret123");
        req.put("code", "123456");
        Optional<String> err = AuthRequestValidator.validateUnifiedLogin(req);
        assertTrue(err.isPresent());
        assertEquals("请求参数异常，请检查 password 或 code", err.get());
    }

    @Test
    void usernamePasswordRegisterValid() {
        Map<String, Object> req = new HashMap<>();
        req.put("credential", "demo_user");
        req.put("password", "secret123");
        assertTrue(AuthRequestValidator.validateUnifiedRegister(req).isEmpty());
    }

    @Test
    void emailLoginWithCodeValid() {
        Map<String, Object> req = new HashMap<>();
        req.put("email", "user@example.com");
        req.put("code", "123456");
        assertEquals(AuthRequestPhase.CODE_AUTH, CredentialValidator.resolvePhase(req));
        assertTrue(AuthRequestValidator.validateUnifiedLogin(req).isEmpty());
    }

    @Test
    void emailLoginWithEmptyCodeRejected() {
        Map<String, Object> req = new HashMap<>();
        req.put("email", "user@example.com");
        req.put("code", "");
        Optional<String> err = AuthRequestValidator.validateUnifiedLogin(req);
        assertTrue(err.isPresent());
        assertEquals("验证码不能为空", err.get());
    }

    @Test
    void emailLoginWithShortPasswordRejected() {
        Map<String, Object> req = new HashMap<>();
        req.put("email", "user@example.com");
        req.put("password", "12345");
        Optional<String> err = AuthRequestValidator.validateUnifiedLogin(req);
        assertTrue(err.isPresent());
        assertEquals("密码长度不能少于6位", err.get());
    }

    @Test
    void emailRegisterWithShortPasswordRejected() {
        Map<String, Object> req = new HashMap<>();
        req.put("email", "user@example.com");
        req.put("password", "12345");
        Optional<String> err = AuthRequestValidator.validateUnifiedRegister(req);
        assertTrue(err.isPresent());
        assertEquals("密码长度不能少于6位", err.get());
    }

    @Test
    void phoneLoginWithCodeReturns501() {
        Map<String, Object> req = new HashMap<>();
        req.put("phone", "13800138000");
        req.put("code", "123456");
        Optional<String> err = AuthRequestValidator.validateUnifiedLogin(req);
        assertTrue(err.isPresent());
        assertEquals(AuthRequestValidator.PHONE_SMS_NOT_CONNECTED, err.get());
    }

    @Test
    void phonePasswordLoginValid() {
        Map<String, Object> req = new HashMap<>();
        req.put("phone", "13800138000");
        req.put("password", "secret123");
        assertTrue(AuthRequestValidator.validateUnifiedLogin(req).isEmpty());
    }
}