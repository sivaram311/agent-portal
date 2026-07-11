package com.agentportal.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "guidance_packs", indexes = {
        @Index(name = "idx_guidance_owner", columnList = "ownerUsername"),
        @Index(name = "idx_guidance_owner_kind", columnList = "ownerUsername, kind")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_guidance_owner_slug", columnNames = {"ownerUsername", "slug"})
})
public class GuidancePack {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 128)
    private String ownerUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GuidanceKind kind = GuidanceKind.RULE;

    @Column(nullable = false, length = 64)
    private String slug;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String description;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String bodyMarkdown;

    /** Comma-separated globs for RULE packs (optional). */
    @Column(length = 512)
    private String globs;

    @Column(nullable = false)
    private boolean alwaysApply = true;

    @Column(nullable = false)
    private boolean enabledByDefault = true;

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

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public GuidanceKind getKind() {
        return kind;
    }

    public void setKind(GuidanceKind kind) {
        this.kind = kind;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBodyMarkdown() {
        return bodyMarkdown;
    }

    public void setBodyMarkdown(String bodyMarkdown) {
        this.bodyMarkdown = bodyMarkdown;
    }

    public String getGlobs() {
        return globs;
    }

    public void setGlobs(String globs) {
        this.globs = globs;
    }

    public boolean isAlwaysApply() {
        return alwaysApply;
    }

    public void setAlwaysApply(boolean alwaysApply) {
        this.alwaysApply = alwaysApply;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    public void setEnabledByDefault(boolean enabledByDefault) {
        this.enabledByDefault = enabledByDefault;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
