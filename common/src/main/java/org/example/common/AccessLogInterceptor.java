package org.example.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class AccessLogInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AccessLogInterceptor.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String START_TIME = "accessLog.startTime";

    private final String serviceName;
    private final AccessLogProperties properties;
    private final AccessLogFileWriter fileWriter;

    public AccessLogInterceptor(String serviceName, AccessLogProperties properties, AccessLogFileWriter fileWriter) {
        this.serviceName = serviceName;
        this.properties = properties;
        this.fileWriter = fileWriter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object startObj = request.getAttribute(START_TIME);
        long startTime = startObj instanceof Long ? (Long) startObj : System.currentTimeMillis();
        long costMs = System.currentTimeMillis() - startTime;
        int httpStatus = response.getStatus();

        ContentCachingRequestWrapper cachedRequest = (ContentCachingRequestWrapper) request.getAttribute(
                AccessLogBodyCaptureFilter.CACHED_REQUEST);
        if (cachedRequest == null && request instanceof ContentCachingRequestWrapper wrapper) {
            cachedRequest = wrapper;
        }
        ContentCachingResponseWrapper cachedResponse = (ContentCachingResponseWrapper) request.getAttribute(
                AccessLogBodyCaptureFilter.CACHED_RESPONSE);

        String reqBodyRaw = readBody(cachedRequest);
        String resBodyRaw = readBody(cachedResponse);
        String bizCode = extractBizCode(resBodyRaw);

        boolean error = httpStatus >= 400 || (bizCode != null && !"0".equals(bizCode));
        String reqParams = buildReqParams(request, reqBodyRaw, error);
        String resBody = buildResBody(resBodyRaw, error);

        AccessLogRecord record = new AccessLogRecord();
        record.setTraceId(MDC.get("traceId"));
        record.setServiceName(serviceName);
        record.setClientIp(resolveClientIp(request));
        record.setMethod(request.getMethod());
        record.setUri(request.getRequestURI());
        record.setCostMs(costMs);
        record.setHttpStatus(httpStatus);
        record.setBizCode(bizCode);
        record.setTimestamp(System.currentTimeMillis());
        record.setReqParams(reqParams);
        record.setResBody(resBody);
        record.setLevel(error ? "WARN" : "INFO");

        fileWriter.write(record);

        if (costMs > properties.getSlowRequestThresholdMs()) {
            log.warn("Slow request service={} uri={} method={} status={} costMs={} traceId={}",
                    serviceName, request.getRequestURI(), request.getMethod(), httpStatus, costMs, record.getTraceId());
        } else {
            log.info("service={} uri={} method={} status={} costMs={} traceId={}",
                    serviceName, request.getRequestURI(), request.getMethod(), httpStatus, costMs, record.getTraceId());
        }
    }

    private String buildReqParams(HttpServletRequest request, String reqBodyRaw, boolean error) {
        if (!properties.isLogBody()) {
            return null;
        }
        if (properties.isBodyOnErrorOnly() && !error) {
            return null;
        }
        Map<String, Object> params = new HashMap<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            params.put(name, request.getParameter(name));
        }
        if (!params.isEmpty()) {
            try {
                return AccessLogMasker.maskAndTruncate(MAPPER.writeValueAsString(params), properties.getMaxBodyLength());
            } catch (Exception e) {
                return AccessLogMasker.maskAndTruncate(params.toString(), properties.getMaxBodyLength());
            }
        }
        if (reqBodyRaw != null && !reqBodyRaw.isBlank()) {
            return AccessLogMasker.maskAndTruncate(reqBodyRaw, properties.getMaxBodyLength());
        }
        return null;
    }

    private String buildResBody(String resBodyRaw, boolean error) {
        if (!properties.isLogBody()) {
            return null;
        }
        if (properties.isBodyOnErrorOnly() && !error) {
            return null;
        }
        if (resBodyRaw == null || resBodyRaw.isBlank()) {
            return null;
        }
        return AccessLogMasker.maskAndTruncate(resBodyRaw, properties.getMaxBodyLength());
    }

    private static String readBody(ContentCachingRequestWrapper request) {
        if (request == null) {
            return null;
        }
        byte[] buf = request.getContentAsByteArray();
        if (buf.length == 0) {
            return null;
        }
        return new String(buf, StandardCharsets.UTF_8);
    }

    private static String readBody(ContentCachingResponseWrapper response) {
        if (response == null) {
            return null;
        }
        byte[] buf = response.getContentAsByteArray();
        if (buf.length == 0) {
            return null;
        }
        return new String(buf, StandardCharsets.UTF_8);
    }

    private static String extractBizCode(String resBodyRaw) {
        if (resBodyRaw == null || resBodyRaw.isBlank()) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(resBodyRaw);
            if (node.has("code")) {
                return String.valueOf(node.get("code").asInt());
            }
        } catch (Exception ignored) {
            // ignore non-json responses
        }
        return null;
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
