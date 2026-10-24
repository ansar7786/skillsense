package com.skillsense.survey.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "participation_log")
public class ParticipationLog {

    @EmbeddedId
    private ParticipationLogId id;

    @Column(name = "has_responded", nullable = false)
    private Boolean hasResponded = false;

    public ParticipationLog() {
    }

    public ParticipationLog(ParticipationLogId id, Boolean hasResponded) {
        this.id = id;
        this.hasResponded = hasResponded != null ? hasResponded : false;
    }

    public ParticipationLog(Long surveyId, Long studentId, Boolean hasResponded) {
        this.id = new ParticipationLogId(surveyId, studentId);
        this.hasResponded = hasResponded != null ? hasResponded : false;
    }

    public ParticipationLogId getId() {
        return id;
    }

    public void setId(ParticipationLogId id) {
        this.id = id;
    }

    public Boolean getHasResponded() {
        return hasResponded;
    }

    public void setHasResponded(Boolean hasResponded) {
        this.hasResponded = hasResponded;
    }
}
