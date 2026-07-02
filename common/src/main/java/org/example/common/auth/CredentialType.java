package org.example.common.auth;

import java.util.Locale;

public enum CredentialType {
    EMAIL,
    PHONE,
    USERNAME;

    public static CredentialType parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return CredentialType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
