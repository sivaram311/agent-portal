package com.agentportal.config;

import com.agentportal.security.CssJwtAuthenticationFilter;
import com.agentportal.security.RateLimitFilter;
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
    private final CssProperties cssProperties;
    private final CssJwtAuthenticationFilter cssJwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(
            AppProperties appProperties,
            CssProperties cssProperties,
            CssJwtAuthenticationFilter cssJwtAuthenticationFilter,
            RateLimitFilter rateLimitFilter
    ) {
        this.appProperties = appProperties;
        this.cssProperties = cssProperties;
        this.cssJwtAuthenticationFilter = cssJwtAuthenticationFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean requireAuth = cssProperties.isEnabled() || appProperties.getSecurity().isEnabled();

        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/api/health",
                            "/api/auth/config",
                            "/api/presets",
                            "/h2-console/**"
                    ).permitAll();
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    if (requireAuth) {
                        if (cssProperties.isEnabled()) {
                            auth.requestMatchers("/ws", "/ws/**").authenticated();
                        } else {
                            auth.requestMatchers("/ws", "/ws/**").permitAll();
                        }
                        auth.requestMatchers("/api/**").authenticated();
                        auth.anyRequest().permitAll();
                    } else {
                        auth.anyRequest().permitAll();
                    }
                })
                .headers(h -> h.frameOptions(f -> f.sameOrigin()));

        http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(cssJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

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
                if (SecurityContextHolder.getContext().getAuthentication() != null) {
                    filterChain.doFilter(request, response);
                    return;
                }
                String path = request.getRequestURI();
                if (path.startsWith("/api/health") || path.startsWith("/api/auth/config")
                        || path.startsWith("/ws") || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
                    filterChain.doFilter(request, response);
                    return;
                }
                if (!path.startsWith("/api/")) {
                    filterChain.doFilter(request, response);
                    return;
                }
                String key = request.getHeader("X-API-Key");
                if (key == null || key.isBlank()) {
                    filterChain.doFilter(request, response);
                    return;
                }
                if (expected.equals(key)) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            "api-key",
                            null,
                            AuthorityUtils.createAuthorityList("ROLE_API")
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
                filterChain.doFilter(request, response);
            }
        };
    }
}
