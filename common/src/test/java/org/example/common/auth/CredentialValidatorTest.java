package org.example.common.auth;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CredentialValidatorTest {

    @Test
    void validEmail() {
        assertTrue(CredentialValidator.validate(CredentialType.EMAIL, "user@example.com").isEmpty());
        assertTrue(CredentialValidator.validate(CredentialType.EMAIL, "User.Name+tag@sub.example.co.uk").isEmpty());
    }

    @Test
    void invalidEmail() {
        assertTrue(CredentialValidator.validate(CredentialType.EMAIL, "not-an-email").isPresent());
        assertTrue(CredentialValidator.validate(CredentialType.EMAIL, "@example.com").isPresent());
    }

    @Test
    void validPhone() {
        assertTrue(CredentialValidator.validate(CredentialType.PHONE, "13800138000").isEmpty());
    }

    @Test
    void invalidPhone() {
        assertTrue(CredentialValidator.validate(CredentialType.PHONE, "12800138000").isPresent());
        assertTrue(CredentialValidator.validate(CredentialType.PHONE, "1380013800").isPresent());
    }

    @Test
    void validUsername() {
        assertTrue(CredentialValidator.validate(CredentialType.USERNAME, "admin").isEmpty());
        assertTrue(CredentialValidator.validate(CredentialType.USERNAME, "demo_user01").isEmpty());
    }

    @Test
    void invalidUsername() {
        assertTrue(CredentialValidator.validate(CredentialType.USERNAME, "1bad").isPresent());
        assertTrue(CredentialValidator.validate(CredentialType.USERNAME, "ab").isPresent());
    }

    @Test
    void normalizeEmailLowercases() {
        assertEquals("user@example.com", CredentialValidator.normalizeEmail("  User@Example.COM "));
    }

    @Test
    void authMethodExclusivity() {
        Map<String, Object> both = new HashMap<>();
        both.put("password", "secret123");
        both.put("code", "123456");
        Optional<String> err = CredentialValidator.validateAuthMethodExclusivity(both);
        assertTrue(err.isPresent());

        Map<String, Object> passwordOnly = new HashMap<>();
        passwordOnly.put("password", "secret123");
        assertEquals(Optional.of(AuthMethod.PASSWORD), CredentialValidator.resolveAuthMethod(passwordOnly));

        Map<String, Object> codeOnly = new HashMap<>();
        codeOnly.put("code", "123456");
        assertEquals(Optional.of(AuthMethod.CODE), CredentialValidator.resolveAuthMethod(codeOnly));
    }

    @Test
    void inferCredentialTypeFromEmailField() {
        Map<String, Object> req = new HashMap<>();
        req.put("email", "user@example.com");
        assertEquals(Optional.of(CredentialType.EMAIL), CredentialValidator.inferCredentialType(req));
    }

    @Test
    void resolvePhaseSendCode() {
        Map<String, Object> req = new HashMap<>();
        req.put("email", "user@example.com");
        assertEquals(AuthRequestPhase.SEND_CODE, CredentialValidator.resolvePhase(req));
    }

    @Test
    void resolvePhasePasswordWithCode() {
        Map<String, Object> req = new HashMap<>();
        req.put("email", "user@example.com");
        req.put("password", "secret123");
        req.put("code", "123456");
        assertEquals(AuthRequestPhase.PASSWORD_WITH_CODE, CredentialValidator.resolvePhase(req));
    }
}
