package org.example.topbiz.config;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class ShiroConfig {

    @Autowired
    private SecurityManager securityManager;

    @Autowired
    private ShiroRealm shiroRealm;

    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean() {
        org.apache.shiro.mgt.DefaultSecurityManager sm =
                (org.apache.shiro.mgt.DefaultSecurityManager) securityManager;
        sm.setRealm(shiroRealm);

        ShiroFilterFactoryBean filterBean = new ShiroFilterFactoryBean();
        filterBean.setSecurityManager(securityManager);

        Map<String, String> filterMap = new LinkedHashMap<>();

        // 公开接口
        filterMap.put("/api/v1/register/**", "anon");
        filterMap.put("/api/v1/login/**", "anon");

        // 管理员接口需要 admin 角色
        filterMap.put("/api/v1/admin/**", "roles[admin]");

        // 其他接口需要登录
        filterMap.put("/api/v1/**", "authc");

        filterBean.setFilterChainDefinitionMap(filterMap);
        return filterBean;
    }
}