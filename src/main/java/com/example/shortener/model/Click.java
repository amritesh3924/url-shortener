package com.example.shortener.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "clicks", indexes = @Index(name = "idx_clicks_short_code", columnList = "short_code"))
public class Click {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, length = 20)
    private String shortCode;

    @Column(name = "clicked_at", nullable = false)
    private Instant clickedAt = Instant.now();

    @Column(length = 500)
    private String referrer;

    @Column(name = "device_type", nullable = false, length = 20)
    private String deviceType;

    public Instant getClickedAt() { return clickedAt; }
    public String getReferrer() { return referrer; }
    public String getDeviceType() { return deviceType; }

    protected Click() { }

    public Click(String shortCode, String referrer, String deviceType) {
        this.shortCode = shortCode;
        this.referrer = referrer;
        this.deviceType = deviceType;
    }
}
