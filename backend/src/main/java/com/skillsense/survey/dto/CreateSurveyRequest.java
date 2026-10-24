package com.skillsense.survey.dto;

import com.skillsense.survey.model.SurveyCategory;
import com.skillsense.survey.model.TargetFilter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class CreateSurveyRequest {

    private Long templateId;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    private String description;

    @NotNull(message = "Category is required")
    private SurveyCategory category;

    @NotNull(message = "Term ID is required")
    private Integer termId;

    @NotNull(message = "Question schema is required")
    private Object questionSchema;

    private Object skillTargets;

    private Boolean isAnonymous = false;

    private TargetFilter targetFilter;

    private LocalDateTime opensAt;

    private LocalDateTime closesAt;

    public CreateSurveyRequest() {
    }

    public CreateSurveyRequest(Long templateId, String title, String description, SurveyCategory category,
                               Integer termId, Object questionSchema, Object skillTargets, Boolean isAnonymous,
                               TargetFilter targetFilter, LocalDateTime opensAt, LocalDateTime closesAt) {
        this.templateId = templateId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.termId = termId;
        this.questionSchema = questionSchema;
        this.skillTargets = skillTargets;
        this.isAnonymous = isAnonymous != null ? isAnonymous : false;
        this.targetFilter = targetFilter;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
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

    public LocalDateTime getClosesAt() {
        return closesAt;
    }

    public void setClosesAt(LocalDateTime closesAt) {
        this.closesAt = closesAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long templateId;
        private String title;
        private String description;
        private SurveyCategory category;
        private Integer termId;
        private Object questionSchema;
        private Object skillTargets;
        private Boolean isAnonymous = false;
        private TargetFilter targetFilter;
        private LocalDateTime opensAt;
        private LocalDateTime closesAt;

        public Builder templateId(Long templateId) {
            this.templateId = templateId;
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

        public Builder closesAt(LocalDateTime closesAt) {
            this.closesAt = closesAt;
            return this;
        }

        public CreateSurveyRequest build() {
            return new CreateSurveyRequest(templateId, title, description, category, termId,
                    questionSchema, skillTargets, isAnonymous, targetFilter, opensAt, closesAt);
        }
    }
}
