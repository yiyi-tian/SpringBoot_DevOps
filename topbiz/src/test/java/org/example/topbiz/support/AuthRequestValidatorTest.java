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
    void emailPasswordWithCodeRegisterValid() {
        Map<String, Object> req = new HashMap<>();
        req.put("email", "user@example.com");
        req.put("password", "secret123");
        req.put("code", "123456");
        assertEquals(AuthRequestPhase.PASSWORD_WITH_CODE, CredentialValidator.resolvePhase(req));
        assertTrue(AuthRequestValidator.validateUnifiedRegister(req).isEmpty());
    }

    @Test
    void emailCodeOnlyRegisterRejected() {
        Map<String, Object> req = new HashMap<>();
        req.put("email", "user@example.com");
        req.put("code", "123456");
        assertEquals(AuthRequestPhase.CODE_AUTH, CredentialValidator.resolvePhase(req));
        Optional<String> err = AuthRequestValidator.validateUnifiedRegister(req);
        assertTrue(err.isPresent());
        assertEquals(AuthRequestValidator.REGISTER_CODE_NEEDS_PASSWORD, err.get());
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
    void loginRejectsPasswordAndCodeTogether() {
        Map<String, Object> req = new HashMap<>();
        req.put("email", "user@example.com");
        req.put("password", "secret123");
        req.put("code", "123456");
        Optional<String> err = AuthRequestValidator.validateUnifiedLogin(req);
        assertTrue(err.isPresent());
    }

    @Test
    void usernamePasswordRegisterValid() {
        Map<String, Object> req = new HashMap<>();
        req.put("credential", "demo_user");
        req.put("password", "secret123");
        assertTrue(AuthRequestValidator.validateUnifiedRegister(req).isEmpty());
    }
}
