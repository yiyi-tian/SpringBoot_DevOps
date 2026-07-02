package org.example.topbiz.config;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.ShiroException;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthorizedException;
import org.example.common.Result;
import org.example.topbiz.exception.InternalServiceException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Shiro 授权异常（子类优先匹配）
    @ExceptionHandler({UnauthorizedException.class, AuthorizationException.class})
    public Result<String> handleAuthorizationException(Exception e) {
        return Result.error(403, "权限不足，无法访问该资源");
    }

    // Shiro 认证异常
    @ExceptionHandler(AuthenticationException.class)
    public Result<String> handleAuthenticationException(AuthenticationException e) {
        return Result.error(401, "未登录或登录已过期，请重新登录");
    }

    // Shiro 其他异常兜底
    @ExceptionHandler(ShiroException.class)
    public Result<String> handleShiroException(ShiroException e) {
        return Result.error(401, "认证异常: " + e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return Result.error(400, e.getMessage());
    }

    @ExceptionHandler(InternalServiceException.class)
    public Result<String> handleInternalServiceException(InternalServiceException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    // HTTP Interface 调用异常
    @ExceptionHandler(WebClientResponseException.class)
    public Result<String> handleWebClientResponseException(WebClientResponseException e) {
        int status = e.getStatusCode().value();
        if (status == 404) {
            return Result.error(503, "服务暂不可用");
        }
        return Result.error(502, "内部服务调用失败，请稍后重试");
    }

    @ExceptionHandler(WebClientRequestException.class)
    public Result<String> handleWebClientRequestException(WebClientRequestException e) {
        return Result.error(502, "内部服务连接失败，请稍后重试");
    }

    // 通用异常
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        e.printStackTrace();
        return Result.error(500, "服务器内部错误");
    }
}