package org.example.common.auth;

/**
 * Unified register/login request phase (single external endpoint).
 */
public enum AuthRequestPhase {
    /** Only credential — send verification code */
    SEND_CODE,
    /** code without password — code-based register/login */
    CODE_AUTH,
    /** password without code — password login or non-email register */
    PASSWORD_AUTH,
    /** password + code — email password register with verification */
    PASSWORD_WITH_CODE
}
