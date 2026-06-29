package org.example.userservice.support;

/**
 * Built-in RBAC group names (must match docs/sql/02b_user_rbac_seed.sql).
 */
public final class RbacDefaults {

    public static final String GROUP_MEMBER = "member";
    public static final String GROUP_ADMIN = "admin";

    public static final long FIRST_ADMIN_USER_ID = 1L;

    private RbacDefaults() {
    }
}
