package org.example.common;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Wraps request/response so AccessLogInterceptor can read bodies after handling.
 */
public class AccessLogBodyCaptureFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AccessLogBodyCaptureFilter.class);

    static final String CACHED_REQUEST = "accessLog.cachedRequest";
    static final String CACHED_RESPONSE = "accessLog.cachedResponse";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        DispatcherType dispatcherType = request.getDispatcherType();
        if (dispatcherType == DispatcherType.ERROR || dispatcherType == DispatcherType.ASYNC) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        wrappedRequest.setAttribute(CACHED_REQUEST, wrappedRequest);
        wrappedRequest.setAttribute(CACHED_RESPONSE, wrappedResponse);
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
            flushCachedBody(response, wrappedResponse);
        } catch (Exception e) {
            if (flushCachedBody(response, wrappedResponse)) {
                return;
            }
            if (!response.isCommitted() && isRedisSessionError(e)) {
                writeJsonError(response, 503, "会话服务暂不可用");
                return;
            }
            if (e instanceof ServletException se) {
                throw se;
            }
            if (e instanceof IOException io) {
                throw io;
            }
            throw new ServletException(e);
        }
    }

    private static boolean isRedisSessionError(Throwable throwable) {
        while (throwable != null) {
            if (throwable.getClass().getName().equals("org.springframework.data.redis.RedisSystemException")) {
                return true;
            }
            throwable = throwable.getCause();
        }
        return false;
    }

    private static void writeJsonError(HttpServletResponse response, int code, String message)
            throws IOException {
        response.setStatus(code);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        String escaped = message.replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + escaped + "\",\"data\":null}");
    }

    private boolean flushCachedBody(HttpServletResponse response, ContentCachingResponseWrapper wrappedResponse)
            throws IOException {
        if (wrappedResponse.getContentSize() == 0) {
            return false;
        }
        try {
            if (!response.isCommitted()) {
                wrappedResponse.copyBodyToResponse();
            }
            return true;
        } catch (IOException | RuntimeException e) {
            log.warn("Response copy or post-commit failed (body may already be sent): {}", e.getMessage());
            return true;
        }
    }
}
