package com.skillsense.analytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "analytics_cache")
public class AnalyticsCache {

    @EmbeddedId
    private AnalyticsCacheId id;

    @Column(name = "response_count", nullable = false)
    private Integer responseCount;

    @Column(name = "payload", nullable = false, columnDefinition = "JSON")
    private String payload;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;

    public AnalyticsCache() {
    }

    public AnalyticsCache(AnalyticsCacheId id, Integer responseCount, String payload, LocalDateTime computedAt) {
        this.id = id;
        this.responseCount = responseCount;
        this.payload = payload;
        this.computedAt = computedAt;
    }

    public AnalyticsCacheId getId() {
        return id;
    }

    public void setId(AnalyticsCacheId id) {
        this.id = id;
    }

    public Integer getResponseCount() {
        return responseCount;
    }

    public void setResponseCount(Integer responseCount) {
        this.responseCount = responseCount;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public LocalDateTime getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(LocalDateTime computedAt) {
        this.computedAt = computedAt;
    }
}
