package org.example.topbiz.config;

import org.apache.shiro.authc.Authenticator;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.authz.Authorizer;
import org.apache.shiro.authz.ModularRealmAuthorizer;
import org.apache.shiro.spring.web.config.DefaultShiroFilterChainDefinition;
import org.apache.shiro.spring.web.config.ShiroFilterChainDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

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
    public ShiroFilterChainDefinition shiroFilterChainDefinition() {
        DefaultShiroFilterChainDefinition chain = new DefaultShiroFilterChainDefinition();
        chain.addPathDefinition("/api/v1/register/**", "anon");
        chain.addPathDefinition("/api/v1/login/**", "anon");
        chain.addPathDefinition("/api/v1/admin/**", "roles[admin]");
        chain.addPathDefinition("/api/v1/log/**", "roles[admin]");
        chain.addPathDefinition("/api/v1/**", "authc");
        return chain;
    }
}
