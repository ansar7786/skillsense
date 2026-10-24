package com.skillsense.analytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_results")
public class StudentResult {

    @EmbeddedId
    private StudentResultId id;

    @Column(name = "payload", nullable = false, columnDefinition = "JSON")
    private String payload;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;

    public StudentResult() {
    }

    public StudentResult(StudentResultId id, String payload, LocalDateTime computedAt) {
        this.id = id;
        this.payload = payload;
        this.computedAt = computedAt;
    }

    public StudentResultId getId() {
        return id;
    }

    public void setId(StudentResultId id) {
        this.id = id;
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
