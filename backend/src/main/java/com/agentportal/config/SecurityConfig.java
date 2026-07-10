package com.agentportal.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AppProperties appProperties;

    public SecurityConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/health", "/h2-console/**", "/ws/**", "/ws").permitAll();
                    if (appProperties.getSecurity().isEnabled()) {
                        auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                        auth.requestMatchers("/api/**").authenticated();
                    } else {
                        auth.anyRequest().permitAll();
                    }
                })
                .headers(h -> h.frameOptions(f -> f.sameOrigin()));

        if (appProperties.getSecurity().isEnabled()) {
            http.addFilterBefore(apiKeyFilter(), UsernamePasswordAuthenticationFilter.class);
        }
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(appProperties.getCors().getAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private OncePerRequestFilter apiKeyFilter() {
        String expected = appProperties.getSecurity().getApiKey();
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain
            ) throws ServletException, IOException {
                String path = request.getRequestURI();
                if (path.startsWith("/api/health") || path.startsWith("/ws") || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
                    filterChain.doFilter(request, response);
                    return;
                }
                if (!path.startsWith("/api/")) {
                    filterChain.doFilter(request, response);
                    return;
                }
                String key = request.getHeader("X-API-Key");
                if (key == null || key.isBlank()) {
                    String auth = request.getHeader("Authorization");
                    if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
                        key = auth.substring(7).trim();
                    }
                }
                if (expected.equals(key)) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            "api-key",
                            null,
                            AuthorityUtils.createAuthorityList("ROLE_API")
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    filterChain.doFilter(request, response);
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Unauthorized — set X-API-Key\"}");
                }
            }
        };
    }
}
