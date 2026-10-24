package com.skillsense.analytics.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardSummaryResponse {

    private Map<String, Long> surveysPerStatus = new HashMap<>();
    private double averageCompletionRate;
    private List<DashboardInsightSummary> latestInsights = new ArrayList<>();

    public DashboardSummaryResponse() {
    }

    public DashboardSummaryResponse(Map<String, Long> surveysPerStatus, double averageCompletionRate, List<DashboardInsightSummary> latestInsights) {
        this.surveysPerStatus = surveysPerStatus;
        this.averageCompletionRate = averageCompletionRate;
        this.latestInsights = latestInsights != null ? latestInsights : new ArrayList<>();
    }

    public Map<String, Long> getSurveysPerStatus() {
        return surveysPerStatus;
    }

    public void setSurveysPerStatus(Map<String, Long> surveysPerStatus) {
        this.surveysPerStatus = surveysPerStatus;
    }

    public double getAverageCompletionRate() {
        return averageCompletionRate;
    }

    public void setAverageCompletionRate(double averageCompletionRate) {
        this.averageCompletionRate = averageCompletionRate;
    }

    public List<DashboardInsightSummary> getLatestInsights() {
        return latestInsights;
    }

    public void setLatestInsights(List<DashboardInsightSummary> latestInsights) {
        this.latestInsights = latestInsights;
    }

    public static class DashboardInsightSummary {
        private Long surveyId;
        private String surveyTitle;
        private String headline;
        private String source;
        private String computedAt;

        public DashboardInsightSummary() {
        }

        public DashboardInsightSummary(Long surveyId, String surveyTitle, String headline, String source, String computedAt) {
            this.surveyId = surveyId;
            this.surveyTitle = surveyTitle;
            this.headline = headline;
            this.source = source;
            this.computedAt = computedAt;
        }

        public Long getSurveyId() {
            return surveyId;
        }

        public void setSurveyId(Long surveyId) {
            this.surveyId = surveyId;
        }

        public String getSurveyTitle() {
            return surveyTitle;
        }

        public void setSurveyTitle(String surveyTitle) {
            this.surveyTitle = surveyTitle;
        }

        public String getHeadline() {
            return headline;
        }

        public void setHeadline(String headline) {
            this.headline = headline;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getComputedAt() {
            return computedAt;
        }

        public void setComputedAt(String computedAt) {
            this.computedAt = computedAt;
        }
    }
}
