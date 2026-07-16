package com.agentportal.security;

import com.agentportal.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window rate limit per client for {@code /api/**} (excluding health / auth config).
 *
 * <p>AgentVerse (and other apps) proxy through Next.js on loopback, so
 * {@link HttpServletRequest#getRemoteAddr()} is often {@code 127.0.0.1} for every user.
 * When the direct peer is loopback/private, honor {@code X-Forwarded-For} / {@code X-Real-Ip} /
 * {@code CF-Connecting-IP} (set by the trusted AV proxy). Prefer authenticated principal when
 * present so users are not merged into one IP bucket.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final int limitPerMinute;
    private final AppProperties appProperties;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @org.springframework.beans.factory.annotation.Value("${app.rate-limit.per-minute:120}") int limitPerMinute,
            AppProperties appProperties
    ) {
        this.limitPerMinute = Math.max(10, limitPerMinute);
        this.appProperties = appProperties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (appProperties.getSecurity().isOpenAccess()) {
            filterChain.doFilter(request, response);
            return;
        }
        String path = request.getRequestURI();
        if (!path.startsWith("/api/") || path.startsWith("/api/health") || path.startsWith("/api/auth/config")
                || path.startsWith("/api/auth/oauth/token")) {
            filterChain.doFilter(request, response);
            return;
        }
        String key = clientKey(request);
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

    static String clientKey(HttpServletRequest request) {
        String user = principalName(request);
        String ip = resolveClientIp(request);
        if (user != null && !user.isBlank()) {
            return "u:" + user + "|ip:" + ip;
        }
        return "ip:" + ip;
    }

    static String resolveClientIp(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if (isTrustedProxyPeer(remote)) {
            String forwarded = firstForwardedIp(request);
            if (forwarded != null) {
                return forwarded;
            }
        }
        return remote != null && !remote.isBlank() ? remote : "unknown";
    }

    private static String firstForwardedIp(HttpServletRequest request) {
        String cf = trimHeader(request.getHeader("CF-Connecting-IP"));
        if (cf != null) {
            return cf;
        }
        String real = trimHeader(request.getHeader("X-Real-IP"));
        if (real != null) {
            return real;
        }
        String xff = trimHeader(request.getHeader("X-Forwarded-For"));
        if (xff == null) {
            return null;
        }
        int comma = xff.indexOf(',');
        return comma < 0 ? xff : xff.substring(0, comma).trim();
    }

    private static String trimHeader(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Trusted peers that may set XFF (Next.js / nginx on this host). */
    static boolean isTrustedProxyPeer(String remote) {
        if (remote == null || remote.isBlank()) {
            return false;
        }
        if ("127.0.0.1".equals(remote) || "https://example.net/id/garnet".equals(remote) || "::1".equals(remote) || "0:0:0:0:0:0:0:1".equals(remote)) {
            return true;
        }
        return remote.startsWith("10.")
                || remote.startsWith("192.168.")
                || isRfc1918_172(remote);
    }

    private static boolean isRfc1918_172(String remote) {
        if (!remote.startsWith("172.")) {
            return false;
        }
        String[] parts = remote.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        try {
            int second = Integer.parseInt(parts[1]);
            return second >= 16 && second <= 31;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String principalName(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null
                && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        // Lightweight (unverified) JWT sub for bucketing when filter runs before JWT parse
        String authz = request.getHeader("Authorization");
        if (authz == null || !authz.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = authz.substring(7).trim();
        return jwtClaim(token, "sub");
    }

    /** Best-effort payload claim read — not for auth decisions. */
    static String jwtClaim(String jwt, String claim) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            String needle = "\"" + claim + "\"";
            int idx = json.indexOf(needle);
            if (idx < 0) {
                return null;
            }
            int colon = json.indexOf(':', idx + needle.length());
            if (colon < 0) {
                return null;
            }
            int startQuote = json.indexOf('"', colon + 1);
            if (startQuote < 0) {
                return null;
            }
            int endQuote = json.indexOf('"', startQuote + 1);
            if (endQuote < 0) {
                return null;
            }
            String value = json.substring(startQuote + 1, endQuote).trim();
            return value.isEmpty() ? null : value;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private record Window(long minute, AtomicInteger count) {
    }
}
