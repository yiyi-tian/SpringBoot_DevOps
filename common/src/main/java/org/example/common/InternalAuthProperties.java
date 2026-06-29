package org.example.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "devops.internal")
public class InternalAuthProperties {

    /** Shared secret for topbiz → internal service calls (header X-Internal-Token). */
    private String token = "";

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isEnforcementEnabled() {
        return token != null && !token.isBlank();
    }
}
