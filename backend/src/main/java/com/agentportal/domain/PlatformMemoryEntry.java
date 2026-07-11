package com.agentportal.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "platform_memory", indexes = {
        @Index(name = "idx_platform_memory_project", columnList = "projectSlug"),
        @Index(name = "idx_platform_memory_kind", columnList = "kind")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_platform_memory_project_key", columnNames = {"projectSlug", "entryKey"})
})
public class PlatformMemoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 128)
    private String projectSlug;

    @Column(nullable = false, length = 200)
    private String entryKey;

    @Column(nullable = false, length = 32)
    private String kind = "NOTE";

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "entry_value", nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(nullable = false, length = 128)
    private String createdBy;

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

    public String getProjectSlug() {
        return projectSlug;
    }

    public void setProjectSlug(String projectSlug) {
        this.projectSlug = projectSlug;
    }

    public String getEntryKey() {
        return entryKey;
    }

    public void setEntryKey(String entryKey) {
        this.entryKey = entryKey;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
