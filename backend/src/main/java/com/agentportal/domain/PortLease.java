package com.agentportal.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "port_leases", indexes = {
        @Index(name = "idx_port_status", columnList = "status")
})
public class PortLease {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private int port;

    @Column(nullable = false, length = 128)
    private String service;

    @Column(nullable = false, length = 128)
    private String ownerApp;

    @Column(nullable = false, length = 32)
    private String env = "host";

    @Column(nullable = false, length = 64)
    private String bindAddress = "0.0.0.0";

    @Column(nullable = false, length = 32)
    private String status = "active";

    @Column(length = 128)
    private String claimedBy;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant claimedAt = Instant.now();

    private Instant releasedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getOwnerApp() {
        return ownerApp;
    }

    public void setOwnerApp(String ownerApp) {
        this.ownerApp = ownerApp;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public void setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getClaimedBy() {
        return claimedBy;
    }

    public void setClaimedBy(String claimedBy) {
        this.claimedBy = claimedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(Instant claimedAt) {
        this.claimedAt = claimedAt;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(Instant releasedAt) {
        this.releasedAt = releasedAt;
    }
}
