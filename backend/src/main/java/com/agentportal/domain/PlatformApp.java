package com.agentportal.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "platform_apps", indexes = {
        @Index(name = "idx_platform_app_env", columnList = "env")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_platform_app_slug", columnNames = {"slug"})
})
public class PlatformApp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String slug;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 128)
    private String clientId;

    @Column(nullable = false, length = 32)
    private String env = "prod";

    @Column(nullable = false, length = 512)
    private String baseUrl;

    @Column(length = 256)
    private String healthUrl;

    @Column(length = 128)
    private String subdomain;

    private Integer upstreamPort;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getHealthUrl() {
        return healthUrl;
    }

    public void setHealthUrl(String healthUrl) {
        this.healthUrl = healthUrl;
    }

    public String getSubdomain() {
        return subdomain;
    }

    public void setSubdomain(String subdomain) {
        this.subdomain = subdomain;
    }

    public Integer getUpstreamPort() {
        return upstreamPort;
    }

    public void setUpstreamPort(Integer upstreamPort) {
        this.upstreamPort = upstreamPort;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
