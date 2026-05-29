package org.example.topbiz.config;

import org.example.common.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        // TODO: 细化异常分类（BIZ/SYS/RPC/DB/AUTH），返回不同的错误码
        return Result.error(500, "服务器内部错误：" + e.getMessage());
    }
}