package com.skillsense.survey.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsense.survey.entity.Survey;
import com.skillsense.survey.model.SurveyCategory;
import com.skillsense.survey.model.SurveyStatus;
import com.skillsense.survey.model.TargetFilter;

import java.time.LocalDateTime;

public class SurveyResponse {

    private Long surveyId;
    private Long templateId;
    private Integer version;
    private String title;
    private String description;
    private SurveyCategory category;
    private Integer termId;
    private String termLabel;
    private Object questionSchema;
    private Object skillTargets;
    private Boolean isAnonymous;
    private TargetFilter targetFilter;
    private SurveyStatus status;
    private LocalDateTime opensAt;
    private LocalDateTime closesAt;
    private Integer eligibleCount;
    private Long responseCount;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime activatedAt;

    public SurveyResponse() {
    }

    public static SurveyResponse fromEntity(Survey s, Long responseCount, ObjectMapper objectMapper) {
        SurveyResponse r = new SurveyResponse();
        r.setSurveyId(s.getSurveyId());
        if (s.getTemplate() != null) {
            r.setTemplateId(s.getTemplate().getTemplateId());
        }
        r.setVersion(s.getVersion());
        r.setTitle(s.getTitle());
        r.setDescription(s.getDescription());
        r.setCategory(s.getCategory());
        if (s.getTerm() != null) {
            r.setTermId(s.getTerm().getTermId());
            r.setTermLabel(s.getTerm().getLabel());
        }
        r.setIsAnonymous(s.getIsAnonymous());
        r.setStatus(s.getStatus());
        r.setOpensAt(s.getOpensAt());
        r.setClosesAt(s.getClosesAt());
        r.setEligibleCount(s.getEligibleCount());
        r.setResponseCount(responseCount != null ? responseCount : 0L);
        if (s.getCreatedBy() != null) {
            r.setCreatedBy(s.getCreatedBy().getUserId());
            r.setCreatedByName(s.getCreatedBy().getName());
        }
        r.setCreatedAt(s.getCreatedAt());
        r.setActivatedAt(s.getActivatedAt());

        // Parse JSON fields if objectMapper is provided
        if (objectMapper != null) {
            try {
                if (s.getQuestionSchema() != null) {
                    r.setQuestionSchema(objectMapper.readTree(s.getQuestionSchema()));
                }
                if (s.getSkillTargets() != null) {
                    r.setSkillTargets(objectMapper.readTree(s.getSkillTargets()));
                }
                if (s.getTargetFilter() != null) {
                    r.setTargetFilter(objectMapper.readValue(s.getTargetFilter(), TargetFilter.class));
                }
            } catch (Exception ignored) {
            }
        }

        return r;
    }

    public Long getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(Long surveyId) {
        this.surveyId = surveyId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
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

    public String getTermLabel() {
        return termLabel;
    }

    public void setTermLabel(String termLabel) {
        this.termLabel = termLabel;
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

    public SurveyStatus getStatus() {
        return status;
    }

    public void setStatus(SurveyStatus status) {
        this.status = status;
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

    public Integer getEligibleCount() {
        return eligibleCount;
    }

    public void setEligibleCount(Integer eligibleCount) {
        this.eligibleCount = eligibleCount;
    }

    public Long getResponseCount() {
        return responseCount;
    }

    public void setResponseCount(Long responseCount) {
        this.responseCount = responseCount;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(LocalDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }
}
