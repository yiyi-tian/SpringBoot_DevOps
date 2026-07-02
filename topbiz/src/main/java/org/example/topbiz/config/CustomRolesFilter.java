package org.example.topbiz.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authz.RolesAuthorizationFilter;
import org.example.common.Result;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CustomRolesFilter extends RolesAuthorizationFilter {

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws IOException {
        Subject subject = getSubject(request, response);
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // 设置响应类型
        httpResponse.setContentType("application/json;charset=UTF-8");
        
        // 区分未登录和权限不足
        if (subject.getPrincipal() == null) {
            // 未登录 → 401
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(httpResponse.getWriter(), 
                Result.error(401, "未登录或登录已过期，请重新登录"));
        } else {
            // 已登录但无权限 → 403
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(httpResponse.getWriter(), 
                Result.error(403, "权限不足，无法访问该资源"));
        }
        
        return false;  // 终止过滤器链
    }
}
