package com.skillsense.survey.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ParticipationLogId implements Serializable {

    @Column(name = "survey_id")
    private Long surveyId;

    @Column(name = "student_id")
    private Long studentId;

    public ParticipationLogId() {
    }

    public ParticipationLogId(Long surveyId, Long studentId) {
        this.surveyId = surveyId;
        this.studentId = studentId;
    }

    public Long getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(Long surveyId) {
        this.surveyId = surveyId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParticipationLogId that = (ParticipationLogId) o;
        return Objects.equals(surveyId, that.surveyId) && Objects.equals(studentId, that.studentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(surveyId, studentId);
    }
}
