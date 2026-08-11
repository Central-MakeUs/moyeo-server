package com.moyeo.global;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);

    public static final String HEADER_NAME = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString();
        long startedAt = System.nanoTime();
        MDC.put(MDC_KEY, traceId);
        response.setHeader(HEADER_NAME, traceId);
        log.info("HTTP request started: method={} path={}", request.getMethod(), request.getRequestURI());

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;
            logCompletion(request, response, durationMillis);
            MDC.remove(MDC_KEY);
        }
    }

    private void logCompletion(HttpServletRequest request, HttpServletResponse response, long durationMillis) {
        int status = response.getStatus();
        if (status >= HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
            log.error(
                    "HTTP request completed: method={} path={} status={} durationMs={}",
                    request.getMethod(), request.getRequestURI(), status, durationMillis
            );
            return;
        }
        if (status >= HttpServletResponse.SC_BAD_REQUEST) {
            log.warn(
                    "HTTP request completed: method={} path={} status={} durationMs={}",
                    request.getMethod(), request.getRequestURI(), status, durationMillis
            );
            return;
        }
        log.info(
                "HTTP request completed: method={} path={} status={} durationMs={}",
                request.getMethod(), request.getRequestURI(), status, durationMillis
        );
    }
}
