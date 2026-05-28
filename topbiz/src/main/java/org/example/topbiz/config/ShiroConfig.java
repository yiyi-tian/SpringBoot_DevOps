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
        // 将自定义 Realm 设置到 SecurityManager
        org.apache.shiro.mgt.DefaultSecurityManager sm =
                (org.apache.shiro.mgt.DefaultSecurityManager) securityManager;
        sm.setRealm(shiroRealm);

        ShiroFilterFactoryBean filterBean = new ShiroFilterFactoryBean();
        filterBean.setSecurityManager(securityManager);

        Map<String, String> filterMap = new LinkedHashMap<>();

        filterMap.put("/api/v1/register/**", "anon");
        filterMap.put("/api/v1/login/**", "anon");
        filterMap.put("/api/v1/**", "authc");

        filterBean.setFilterChainDefinitionMap(filterMap);
        return filterBean;
    }
}