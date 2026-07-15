package com.agentportal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "css")
public class CssProperties {

    /** When true, require a valid CSS JWT (or legacy API key if configured). */
    private boolean enabled = false;
    private String authUrl = "http://localhost:9000";
    private String jwksUri = "http://localhost:9000/.well-known/jwks.json";
    private String issuer = "http://localhost:9000";
    private String clientId = "agent-portal";
    private long jwksCacheSeconds = 3600;
    /**
     * password = form only; hybrid/oauth = form + CSS SSO (authorize / PKCE).
     * Env: CSS_AUTH_MODE.
     */
    private String authMode = "hybrid";
    /** Browser callback path (must NOT be under nginx /auth/ → IdP). */
    private String oauthRedirectPath = "/oauth/callback";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public void setAuthUrl(String authUrl) {
        this.authUrl = authUrl;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public long getJwksCacheSeconds() {
        return jwksCacheSeconds;
    }

    public void setJwksCacheSeconds(long jwksCacheSeconds) {
        this.jwksCacheSeconds = jwksCacheSeconds;
    }

    public String getAuthMode() {
        return authMode;
    }

    public void setAuthMode(String authMode) {
        this.authMode = authMode;
    }

    public String getOauthRedirectPath() {
        return oauthRedirectPath;
    }

    public void setOauthRedirectPath(String oauthRedirectPath) {
        this.oauthRedirectPath = oauthRedirectPath;
    }
}
