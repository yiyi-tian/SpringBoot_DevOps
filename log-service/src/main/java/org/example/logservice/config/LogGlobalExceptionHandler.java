package org.example.logservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class LogGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(LogGlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public Map<String, Object> handleBadRequest(IllegalArgumentException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 400);
        result.put("message", e.getMessage());
        return result;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Map<String, Object> handleUnreadableBody(HttpMessageNotReadableException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 400);
        result.put("message", "请求体 JSON 格式无效");
        return result;
    }

    @ExceptionHandler(Exception.class)
    public Map<String, Object> handleException(Exception e) {
        log.error("Request failed", e);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 500);
        result.put("message", e.getMessage());
        return result;
    }
}
