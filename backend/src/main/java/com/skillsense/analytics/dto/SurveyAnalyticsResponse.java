package com.skillsense.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

public class SurveyAnalyticsResponse {

    private boolean available;
    private long have;
    private int need;
    private Object data;

    public SurveyAnalyticsResponse() {
    }

    public SurveyAnalyticsResponse(boolean available, long have, int need, Object data) {
        this.available = available;
        this.have = have;
        this.need = need;
        this.data = data != null ? data : Collections.emptyMap();
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public long getHave() {
        return have;
    }

    public void setHave(long have) {
        this.have = have;
    }

    public int getNeed() {
        return need;
    }

    public void setNeed(int need) {
        this.need = need;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
