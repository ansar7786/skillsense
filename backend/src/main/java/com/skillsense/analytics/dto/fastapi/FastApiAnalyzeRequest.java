package com.skillsense.analytics.dto.fastapi;

import java.util.List;

public class FastApiAnalyzeRequest {

    private FastApiSurveyMeta survey;
    private List<FastApiResponseRow> responses;

    public FastApiAnalyzeRequest() {
    }

    public FastApiAnalyzeRequest(FastApiSurveyMeta survey, List<FastApiResponseRow> responses) {
        this.survey = survey;
        this.responses = responses;
    }

    public FastApiSurveyMeta getSurvey() {
        return survey;
    }

    public void setSurvey(FastApiSurveyMeta survey) {
        this.survey = survey;
    }

    public List<FastApiResponseRow> getResponses() {
        return responses;
    }

    public void setResponses(List<FastApiResponseRow> responses) {
        this.responses = responses;
    }
}
