package com.example.jwtapi.security;

import java.time.Instant;

import com.example.jwtapi.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(length = 120)
    private String deviceName;

    @Column(length = 512)
    private String userAgent;

    @Column(length = 45)
    private String ipAddress;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant lastUsedAt;

    private Instant revokedAt;

    @Column(nullable = false)
    private boolean revoked;

    protected RefreshToken() {
    }

    public RefreshToken(
            User user,
            String tokenHash,
            String deviceName,
            String userAgent,
            String ipAddress,
            Instant createdAt,
            Instant expiresAt
    ) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.deviceName = deviceName;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void revoke() {
        revoke(Instant.now());
    }

    public void revoke(Instant revokedAt) {
        this.revoked = true;
        this.revokedAt = revokedAt;
    }

    public void markUsed(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
