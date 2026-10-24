package com.skillsense.response.dto;

import com.skillsense.survey.model.SurveyCategory;

import java.time.LocalDateTime;

public class StudentSurveySummaryResponse {

    private Long surveyId;
    private String title;
    private SurveyCategory category;
    private LocalDateTime closesAt;
    private Boolean isAnonymous;
    private String status; // "PENDING" or "SUBMITTED"

    public StudentSurveySummaryResponse() {
    }

    public StudentSurveySummaryResponse(Long surveyId, String title, SurveyCategory category,
                                        LocalDateTime closesAt, Boolean isAnonymous, String status) {
        this.surveyId = surveyId;
        this.title = title;
        this.category = category;
        this.closesAt = closesAt;
        this.isAnonymous = isAnonymous;
        this.status = status;
    }

    public Long getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(Long surveyId) {
        this.surveyId = surveyId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public SurveyCategory getCategory() {
        return category;
    }

    public void setCategory(SurveyCategory category) {
        this.category = category;
    }

    public LocalDateTime getClosesAt() {
        return closesAt;
    }

    public void setClosesAt(LocalDateTime closesAt) {
        this.closesAt = closesAt;
    }

    public Boolean getIsAnonymous() {
        return isAnonymous;
    }

    public void setIsAnonymous(Boolean isAnonymous) {
        this.isAnonymous = isAnonymous;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long surveyId;
        private String title;
        private SurveyCategory category;
        private LocalDateTime closesAt;
        private Boolean isAnonymous;
        private String status;

        public Builder surveyId(Long surveyId) {
            this.surveyId = surveyId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder category(SurveyCategory category) {
            this.category = category;
            return this;
        }

        public Builder closesAt(LocalDateTime closesAt) {
            this.closesAt = closesAt;
            return this;
        }

        public Builder isAnonymous(Boolean isAnonymous) {
            this.isAnonymous = isAnonymous;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public StudentSurveySummaryResponse build() {
            return new StudentSurveySummaryResponse(surveyId, title, category, closesAt, isAnonymous, status);
        }
    }
}
