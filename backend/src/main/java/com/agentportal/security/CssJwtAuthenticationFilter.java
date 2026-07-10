package com.agentportal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CssJwtAuthenticationFilter extends OncePerRequestFilter {

    private final CssJwtValidator cssJwtValidator;

    public CssJwtAuthenticationFilter(CssJwtValidator cssJwtValidator) {
        this.cssJwtValidator = cssJwtValidator;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!cssJwtValidator.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = parseJwt(request);
        if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            cssJwtValidator.authenticate(jwt).ifPresent(authToken -> {
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            });
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        if (isWebSocketUpgrade(request)) {
            String queryToken = request.getParameter("access_token");
            if (StringUtils.hasText(queryToken)) {
                return queryToken;
            }
        }
        return null;
    }

    private boolean isWebSocketUpgrade(HttpServletRequest request) {
        String upgrade = request.getHeader("Upgrade");
        return StringUtils.hasText(upgrade) && "websocket".equalsIgnoreCase(upgrade);
    }
}
