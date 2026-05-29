package org.example.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 访问日志拦截器：记录每次请求的耗时、状态码等信息
 * TODO: 将日志写入 logs/access/ 目录的 JSON 文件，由 Vector 采集到 ClickHouse
 */
public class AccessLogInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AccessLogInterceptor.class);

    private final String serviceName;

    public AccessLogInterceptor(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("startTime", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        long startTime = (Long) request.getAttribute("startTime");
        long costMs = System.currentTimeMillis() - startTime;

        // TODO: 构建 JSON 日志对象，包含 trace_id, service_name, client_ip, method, uri, cost_ms, http_status 等字段
        // TODO: 将 JSON 写入 logs/access/ 目录，文件名按日期滚动

        log.info("service={} uri={} method={} status={} costMs={}",
                serviceName, request.getRequestURI(), request.getMethod(), response.getStatus(), costMs);
    }
}