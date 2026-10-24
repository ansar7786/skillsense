package com.skillsense.analytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class StudentResultId implements Serializable {

    @Column(name = "survey_id", nullable = false)
    private Long surveyId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    public StudentResultId() {
    }

    public StudentResultId(Long surveyId, Long studentId) {
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
        StudentResultId that = (StudentResultId) o;
        return Objects.equals(surveyId, that.surveyId) && Objects.equals(studentId, that.studentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(surveyId, studentId);
    }
}
