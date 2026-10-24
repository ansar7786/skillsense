package com.skillsense.analytics.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class FastApiSurveyMeta {

    private Long id;
    private String category;

    @JsonProperty("is_anonymous")
    private Boolean isAnonymous;

    @JsonProperty("eligible_count")
    private Integer eligibleCount;

    @JsonProperty("question_schema")
    private Object questionSchema;

    @JsonProperty("skill_targets")
    private Map<String, Double> skillTargets;

    public FastApiSurveyMeta() {
    }

    public FastApiSurveyMeta(Long id, String category, Boolean isAnonymous, Integer eligibleCount, Object questionSchema, Map<String, Double> skillTargets) {
        this.id = id;
        this.category = category;
        this.isAnonymous = isAnonymous;
        this.eligibleCount = eligibleCount;
        this.questionSchema = questionSchema;
        this.skillTargets = skillTargets;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getIsAnonymous() {
        return isAnonymous;
    }

    public void setIsAnonymous(Boolean isAnonymous) {
        this.isAnonymous = isAnonymous;
    }

    public Integer getEligibleCount() {
        return eligibleCount;
    }

    public void setEligibleCount(Integer eligibleCount) {
        this.eligibleCount = eligibleCount;
    }

    public Object getQuestionSchema() {
        return questionSchema;
    }

    public void setQuestionSchema(Object questionSchema) {
        this.questionSchema = questionSchema;
    }

    public Map<String, Double> getSkillTargets() {
        return skillTargets;
    }

    public void setSkillTargets(Map<String, Double> skillTargets) {
        this.skillTargets = skillTargets;
    }
}
