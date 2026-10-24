package com.skillsense.survey.dto;

import com.skillsense.survey.model.SurveyCategory;
import com.skillsense.survey.model.TargetFilter;

import java.time.LocalDateTime;

public class UpdateSurveyRequest {

    private String title;
    private String description;
    private LocalDateTime closesAt;

    // DRAFT-only editable fields:
    private SurveyCategory category;
    private Integer termId;
    private Object questionSchema;
    private Object skillTargets;
    private Boolean isAnonymous;
    private TargetFilter targetFilter;
    private LocalDateTime opensAt;

    public UpdateSurveyRequest() {
    }

    public UpdateSurveyRequest(String title, String description, LocalDateTime closesAt, SurveyCategory category,
                               Integer termId, Object questionSchema, Object skillTargets, Boolean isAnonymous,
                               TargetFilter targetFilter, LocalDateTime opensAt) {
        this.title = title;
        this.description = description;
        this.closesAt = closesAt;
        this.category = category;
        this.termId = termId;
        this.questionSchema = questionSchema;
        this.skillTargets = skillTargets;
        this.isAnonymous = isAnonymous;
        this.targetFilter = targetFilter;
        this.opensAt = opensAt;
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

    public LocalDateTime getClosesAt() {
        return closesAt;
    }

    public void setClosesAt(LocalDateTime closesAt) {
        this.closesAt = closesAt;
    }

    public SurveyCategory getCategory() {
        return category;
    }

    public void setCategory(SurveyCategory category) {
        this.category = category;
    }

    public Integer getTermId() {
        return termId;
    }

    public void setTermId(Integer termId) {
        this.termId = termId;
    }

    public Object getQuestionSchema() {
        return questionSchema;
    }

    public void setQuestionSchema(Object questionSchema) {
        this.questionSchema = questionSchema;
    }

    public Object getSkillTargets() {
        return skillTargets;
    }

    public void setSkillTargets(Object skillTargets) {
        this.skillTargets = skillTargets;
    }

    public Boolean getIsAnonymous() {
        return isAnonymous;
    }

    public void setIsAnonymous(Boolean isAnonymous) {
        this.isAnonymous = isAnonymous;
    }

    public TargetFilter getTargetFilter() {
        return targetFilter;
    }

    public void setTargetFilter(TargetFilter targetFilter) {
        this.targetFilter = targetFilter;
    }

    public LocalDateTime getOpensAt() {
        return opensAt;
    }

    public void setOpensAt(LocalDateTime opensAt) {
        this.opensAt = opensAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title;
        private String description;
        private LocalDateTime closesAt;
        private SurveyCategory category;
        private Integer termId;
        private Object questionSchema;
        private Object skillTargets;
        private Boolean isAnonymous;
        private TargetFilter targetFilter;
        private LocalDateTime opensAt;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder closesAt(LocalDateTime closesAt) {
            this.closesAt = closesAt;
            return this;
        }

        public Builder category(SurveyCategory category) {
            this.category = category;
            return this;
        }

        public Builder termId(Integer termId) {
            this.termId = termId;
            return this;
        }

        public Builder questionSchema(Object questionSchema) {
            this.questionSchema = questionSchema;
            return this;
        }

        public Builder skillTargets(Object skillTargets) {
            this.skillTargets = skillTargets;
            return this;
        }

        public Builder isAnonymous(Boolean isAnonymous) {
            this.isAnonymous = isAnonymous;
            return this;
        }

        public Builder targetFilter(TargetFilter targetFilter) {
            this.targetFilter = targetFilter;
            return this;
        }

        public Builder opensAt(LocalDateTime opensAt) {
            this.opensAt = opensAt;
            return this;
        }

        public UpdateSurveyRequest build() {
            return new UpdateSurveyRequest(title, description, closesAt, category, termId,
                    questionSchema, skillTargets, isAnonymous, targetFilter, opensAt);
        }
    }
}
