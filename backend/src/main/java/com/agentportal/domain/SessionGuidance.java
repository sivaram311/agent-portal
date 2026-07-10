package com.agentportal.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "session_guidance", indexes = {
        @Index(name = "idx_session_guidance_session", columnList = "sessionId")
})
public class SessionGuidance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID sessionId;

    /** Library pack reference; null when session-only body is used. */
    private UUID packId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GuidanceKind kind = GuidanceKind.RULE;

    @Column(length = 200)
    private String title;

    /** Session-only markdown when packId is null; otherwise unused (body comes from pack). */
    @Lob
    @Column(columnDefinition = "CLOB")
    private String sessionBody;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private int sortOrder = 0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public UUID getPackId() {
        return packId;
    }

    public void setPackId(UUID packId) {
        this.packId = packId;
    }

    public GuidanceKind getKind() {
        return kind;
    }

    public void setKind(GuidanceKind kind) {
        this.kind = kind;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSessionBody() {
        return sessionBody;
    }

    public void setSessionBody(String sessionBody) {
        this.sessionBody = sessionBody;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
