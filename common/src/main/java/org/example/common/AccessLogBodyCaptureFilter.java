package org.example.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * Wraps request/response so AccessLogInterceptor can read bodies after handling.
 */
public class AccessLogBodyCaptureFilter extends OncePerRequestFilter {

    static final String CACHED_REQUEST = "accessLog.cachedRequest";
    static final String CACHED_RESPONSE = "accessLog.cachedResponse";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        wrappedRequest.setAttribute(CACHED_REQUEST, wrappedRequest);
        wrappedRequest.setAttribute(CACHED_RESPONSE, wrappedResponse);
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            wrappedResponse.copyBodyToResponse();
        }
    }
}
