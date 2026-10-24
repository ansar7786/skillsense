package com.skillsense.analytics.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RecommendationItem {

    private int priority;
    private String title;
    private String action;
    private String rationale;

    @JsonProperty("suggested_owner")
    @JsonAlias({"suggestedOwner", "suggested_owner"})
    private String suggestedOwner;

    public RecommendationItem() {
    }

    public RecommendationItem(int priority, String title, String action, String rationale, String suggestedOwner) {
        this.priority = priority;
        this.title = title;
        this.action = action;
        this.rationale = rationale;
        this.suggestedOwner = suggestedOwner;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public String getSuggestedOwner() {
        return suggestedOwner;
    }

    public void setSuggestedOwner(String suggestedOwner) {
        this.suggestedOwner = suggestedOwner;
    }
}
