package com.skillsense.analytics.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class FastApiStudentResultRequest {

    private FastApiSurveyMeta survey;
    private Map<String, Object> answers;

    @JsonProperty("cohort_means")
    private Map<String, Double> cohortMeans;

    public FastApiStudentResultRequest() {
    }

    public FastApiStudentResultRequest(FastApiSurveyMeta survey, Map<String, Object> answers, Map<String, Double> cohortMeans) {
        this.survey = survey;
        this.answers = answers;
        this.cohortMeans = cohortMeans;
    }

    public FastApiSurveyMeta getSurvey() {
        return survey;
    }

    public void setSurvey(FastApiSurveyMeta survey) {
        this.survey = survey;
    }

    public Map<String, Object> getAnswers() {
        return answers;
    }

    public void setAnswers(Map<String, Object> answers) {
        this.answers = answers;
    }

    public Map<String, Double> getCohortMeans() {
        return cohortMeans;
    }

    public void setCohortMeans(Map<String, Double> cohortMeans) {
        this.cohortMeans = cohortMeans;
    }
}
