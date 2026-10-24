package com.skillsense.response.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.skillsense.survey.model.SurveyCategory;

import java.time.LocalDateTime;

public class StudentSurveyDetailResponse {

    private Long surveyId;
    private String title;
    private String description;
    private SurveyCategory category;
    private Boolean isAnonymous;
    private String anonymityNotice;
    private LocalDateTime closesAt;
    private JsonNode questionSchema; // Sanitized: NO scoring, weights, or targets

    public StudentSurveyDetailResponse() {
    }

    public StudentSurveyDetailResponse(Long surveyId, String title, String description, SurveyCategory category,
                                       Boolean isAnonymous, String anonymityNotice, LocalDateTime closesAt,
                                       JsonNode questionSchema) {
        this.surveyId = surveyId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.isAnonymous = isAnonymous;
        this.anonymityNotice = anonymityNotice;
        this.closesAt = closesAt;
        this.questionSchema = questionSchema;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SurveyCategory getCategory() {
        return category;
    }

    public void setCategory(SurveyCategory category) {
        this.category = category;
    }

    public Boolean getIsAnonymous() {
        return isAnonymous;
    }

    public void setIsAnonymous(Boolean isAnonymous) {
        this.isAnonymous = isAnonymous;
    }

    public String getAnonymityNotice() {
        return anonymityNotice;
    }

    public void setAnonymityNotice(String anonymityNotice) {
        this.anonymityNotice = anonymityNotice;
    }

    public LocalDateTime getClosesAt() {
        return closesAt;
    }

    public void setClosesAt(LocalDateTime closesAt) {
        this.closesAt = closesAt;
    }

    public JsonNode getQuestionSchema() {
        return questionSchema;
    }

    public void setQuestionSchema(JsonNode questionSchema) {
        this.questionSchema = questionSchema;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long surveyId;
        private String title;
        private String description;
        private SurveyCategory category;
        private Boolean isAnonymous;
        private String anonymityNotice;
        private LocalDateTime closesAt;
        private JsonNode questionSchema;

        public Builder surveyId(Long surveyId) {
            this.surveyId = surveyId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder category(SurveyCategory category) {
            this.category = category;
            return this;
        }

        public Builder isAnonymous(Boolean isAnonymous) {
            this.isAnonymous = isAnonymous;
            return this;
        }

        public Builder anonymityNotice(String anonymityNotice) {
            this.anonymityNotice = anonymityNotice;
            return this;
        }

        public Builder closesAt(LocalDateTime closesAt) {
            this.closesAt = closesAt;
            return this;
        }

        public Builder questionSchema(JsonNode questionSchema) {
            this.questionSchema = questionSchema;
            return this;
        }

        public StudentSurveyDetailResponse build() {
            return new StudentSurveyDetailResponse(surveyId, title, description, category, isAnonymous, anonymityNotice, closesAt, questionSchema);
        }
    }
}
