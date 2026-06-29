package org.example.topbiz.support;

import org.apache.shiro.SecurityUtils;

public final class SecurityUtilsHelper {

    private SecurityUtilsHelper() {
    }

    public static Long getCurrentUserId() {
        try {
            Object principal = SecurityUtils.getSubject().getPrincipal();
            if (principal == null) {
                return null;
            }
            return Long.valueOf(String.valueOf(principal));
        } catch (Exception e) {
            return null;
        }
    }
}
