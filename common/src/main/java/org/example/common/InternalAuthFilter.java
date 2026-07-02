package org.example.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

public class InternalAuthFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Internal-Token";

    private final InternalAuthProperties properties;
    private final Environment environment;

    public InternalAuthFilter(InternalAuthProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (shouldSkipAuth()) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(HEADER_NAME);
        if (header == null || !properties.getToken().equals(header)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"Forbidden: invalid internal token\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkipAuth() {
        if (properties.isEnforcementEnabled()) {
            return false;
        }
        return Arrays.stream(environment.getActiveProfiles())
                .noneMatch(this::isStrictProfile);
    }

    private boolean isStrictProfile(String profile) {
        return "docker".equals(profile) || "kubernetes".equals(profile) || "k8s".equals(profile);
    }
}
