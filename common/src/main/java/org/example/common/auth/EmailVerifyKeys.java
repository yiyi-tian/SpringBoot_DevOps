package org.example.common.auth;

import java.time.Duration;

/**
 * Redis keys for email verification flow (shared by message-service and topbiz).
 */
public final class EmailVerifyKeys {

    public static final Duration VERIFIED_TTL = Duration.ofMinutes(10);

    private EmailVerifyKeys() {
    }

    public static String verifiedKey(String scene, String email) {
        return "verify:email:" + scene + ":verified:" + CredentialValidator.normalizeEmail(email);
    }
}
