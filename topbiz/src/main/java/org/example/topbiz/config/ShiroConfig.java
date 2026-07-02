package org.example.topbiz.config;

import jakarta.servlet.Filter;
import org.apache.shiro.authc.Authenticator;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.authz.Authorizer;
import org.apache.shiro.authz.ModularRealmAuthorizer;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class ShiroConfig {

    @Bean
    public Authorizer authorizer(ShiroRealm shiroRealm) {
        ModularRealmAuthorizer authorizer = new ModularRealmAuthorizer();
        authorizer.setRealms(Collections.singletonList(shiroRealm));
        return authorizer;
    }

    @Bean
    public Authenticator authenticator(ShiroRealm shiroRealm) {
        ModularRealmAuthenticator authenticator = new ModularRealmAuthenticator();
        authenticator.setRealms(Collections.singletonList(shiroRealm));
        return authenticator;
    }

    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean(org.apache.shiro.mgt.SecurityManager securityManager) {
        ShiroFilterFactoryBean factoryBean = new ShiroFilterFactoryBean();
        factoryBean.setSecurityManager(securityManager);

        // 注册自定义过滤器，覆盖默认的 roles
        Map<String, Filter> filters = new LinkedHashMap<>();
        filters.put("roles", new CustomRolesFilter());
        factoryBean.setFilters(filters);

        // 过滤器链
        Map<String, String> chain = new LinkedHashMap<>();
        chain.put("/api/v1/register", "anon");
        chain.put("/api/v1/login", "anon");
        chain.put("/api/v1/password/reset/**", "anon");

        chain.put("/api/v1/admin/**", "roles[admin]");
        chain.put("/api/v1/log/**", "roles[admin]");
        chain.put("/api/v1/templates/**", "roles[admin]");
        chain.put("/api/v1/variables/**", "roles[admin]");
        chain.put("/api/v1/msg/carriers/**", "roles[admin]");

        chain.put("/api/v1/send/**", "authc");
        chain.put("/api/v1/messages/**", "authc");
        chain.put("/api/v1/**", "authc");

        factoryBean.setFilterChainDefinitionMap(chain);
        return factoryBean;
    }
}