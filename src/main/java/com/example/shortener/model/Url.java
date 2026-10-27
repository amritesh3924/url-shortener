package com.example.shortener.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "urls", indexes = @Index(name = "idx_urls_short_code", columnList = "short_code", unique = true))
public class Url {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", length = 20, unique = true)
    private String shortCode;

    @Column(name = "long_url", length = 2048, nullable = false)
    private String longUrl;

    @Column(name = "click_count", nullable = false)
    private long clickCount = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected Url() { }

    public Url(String longUrl, Instant expiresAt) {
        this.longUrl = longUrl;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }
    public String getLongUrl() { return longUrl; }
    public long getClickCount() { return clickCount; }
    public Instant getExpiresAt() { return expiresAt; }
}
