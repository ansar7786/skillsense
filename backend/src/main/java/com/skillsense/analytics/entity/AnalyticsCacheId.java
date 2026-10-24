package com.skillsense.analytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class AnalyticsCacheId implements Serializable {

    @Column(name = "survey_id", nullable = false)
    private Long surveyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private AnalyticsCacheKind kind;

    public AnalyticsCacheId() {
    }

    public AnalyticsCacheId(Long surveyId, AnalyticsCacheKind kind) {
        this.surveyId = surveyId;
        this.kind = kind;
    }

    public Long getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(Long surveyId) {
        this.surveyId = surveyId;
    }

    public AnalyticsCacheKind getKind() {
        return kind;
    }

    public void setKind(AnalyticsCacheKind kind) {
        this.kind = kind;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalyticsCacheId that = (AnalyticsCacheId) o;
        return Objects.equals(surveyId, that.surveyId) && kind == that.kind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(surveyId, kind);
    }
}
