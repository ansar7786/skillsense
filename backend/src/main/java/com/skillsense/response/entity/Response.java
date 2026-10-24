package com.skillsense.response.entity;

import com.skillsense.survey.entity.Survey;
import com.skillsense.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "responses")
public class Response {

    @Id
    @Column(name = "response_id", length = 36, nullable = false)
    private String responseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private User student;

    @Column(name = "group_branch", length = 20)
    private String groupBranch;

    @Column(name = "group_year")
    private Integer groupYear;

    @Column(name = "group_section", length = 5)
    private String groupSection;

    @Column(name = "answer_data", nullable = false, columnDefinition = "JSON")
    private String answerData;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    public Response() {
    }

    public Response(String responseId, Survey survey, User student, String groupBranch, Integer groupYear,
                    String groupSection, String answerData, LocalDateTime submittedAt) {
        this.responseId = responseId;
        this.survey = survey;
        this.student = student;
        this.groupBranch = groupBranch;
        this.groupYear = groupYear;
        this.groupSection = groupSection;
        this.answerData = answerData;
        this.submittedAt = submittedAt;
    }

    public String getResponseId() {
        return responseId;
    }

    public void setResponseId(String responseId) {
        this.responseId = responseId;
    }

    public Survey getSurvey() {
        return survey;
    }

    public void setSurvey(Survey survey) {
        this.survey = survey;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public String getGroupBranch() {
        return groupBranch;
    }

    public void setGroupBranch(String groupBranch) {
        this.groupBranch = groupBranch;
    }

    public Integer getGroupYear() {
        return groupYear;
    }

    public void setGroupYear(Integer groupYear) {
        this.groupYear = groupYear;
    }

    public String getGroupSection() {
        return groupSection;
    }

    public void setGroupSection(String groupSection) {
        this.groupSection = groupSection;
    }

    public String getAnswerData() {
        return answerData;
    }

    public void setAnswerData(String answerData) {
        this.answerData = answerData;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String responseId;
        private Survey survey;
        private User student;
        private String groupBranch;
        private Integer groupYear;
        private String groupSection;
        private String answerData;
        private LocalDateTime submittedAt;

        public Builder responseId(String responseId) {
            this.responseId = responseId;
            return this;
        }

        public Builder survey(Survey survey) {
            this.survey = survey;
            return this;
        }

        public Builder student(User student) {
            this.student = student;
            return this;
        }

        public Builder groupBranch(String groupBranch) {
            this.groupBranch = groupBranch;
            return this;
        }

        public Builder groupYear(Integer groupYear) {
            this.groupYear = groupYear;
            return this;
        }

        public Builder groupSection(String groupSection) {
            this.groupSection = groupSection;
            return this;
        }

        public Builder answerData(String answerData) {
            this.answerData = answerData;
            return this;
        }

        public Builder submittedAt(LocalDateTime submittedAt) {
            this.submittedAt = submittedAt;
            return this;
        }

        public Response build() {
            return new Response(responseId, survey, student, groupBranch, groupYear, groupSection, answerData, submittedAt);
        }
    }
}
