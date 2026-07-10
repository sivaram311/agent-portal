package com.agentportal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple fixed-window rate limit per client IP for /api/** (excluding health).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final int limitPerMinute;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${app.rate-limit.per-minute:120}") int limitPerMinute) {
        this.limitPerMinute = Math.max(10, limitPerMinute);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/") || path.startsWith("/api/health") || path.startsWith("/api/auth/config")) {
            filterChain.doFilter(request, response);
            return;
        }
        String key = request.getRemoteAddr();
        long minute = System.currentTimeMillis() / 60_000L;
        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || existing.minute != minute) {
                return new Window(minute, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });
        if (window.count.get() > limitPerMinute) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private record Window(long minute, AtomicInteger count) {
    }
}
