package org.example.topbiz.config;

import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.example.topbiz.feign.UserServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
@Component
public class ShiroRealm extends AuthorizingRealm {

    @Autowired
    private UserServiceClient userServiceClient;

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token)
            throws AuthenticationException {
        String userId = (String) token.getPrincipal();
        return new SimpleAuthenticationInfo(userId, token.getCredentials(), getName());
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();

        try {
            String userId = (String) principals.getPrimaryPrincipal();
            // 调用 user-service 查询用户权限
            Map<String, Object> result = userServiceClient.getPermissions(Long.valueOf(userId));
            if (result != null && "0".equals(String.valueOf(result.get("code")))) {
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                if (data != null) {
                    List<String> permissions = (List<String>) data.get("permissions");
                    if (permissions != null) {
                        info.addStringPermissions(permissions);
                    }
                    List<String> roles = (List<String>) data.get("roles");
                    if (roles != null) {
                        roles.forEach(info::addRole);
                    }
                    if (Boolean.TRUE.equals(data.get("is_admin"))) {
                        info.addRole("admin");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("查询用户权限失败: " + e.getMessage());
        }

        return info;
    }
}