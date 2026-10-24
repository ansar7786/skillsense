package com.skillsense.analytics.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FastApiInsightsRequest {

    private Object aggregate;
    private Object trend;
    private String category;

    @JsonProperty("allow_ai")
    private Boolean allowAi;

    public FastApiInsightsRequest() {
    }

    public FastApiInsightsRequest(Object aggregate, Object trend, String category, Boolean allowAi) {
        this.aggregate = aggregate;
        this.trend = trend;
        this.category = category;
        this.allowAi = allowAi;
    }

    public Object getAggregate() {
        return aggregate;
    }

    public void setAggregate(Object aggregate) {
        this.aggregate = aggregate;
    }

    public Object getTrend() {
        return trend;
    }

    public void setTrend(Object trend) {
        this.trend = trend;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getAllowAi() {
        return allowAi;
    }

    public void setAllowAi(Boolean allowAi) {
        this.allowAi = allowAi;
    }
}
