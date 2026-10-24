package com.skillsense.analytics.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class InsightResponse {

    private String headline;

    @JsonProperty("key_findings")
    @JsonAlias({"keyFindings", "key_findings"})
    private List<String> keyFindings = new ArrayList<>();

    private List<RecommendationItem> recommendations = new ArrayList<>();

    @JsonProperty("watch_outs")
    @JsonAlias({"watchOuts", "watch_outs"})
    private List<String> watchOuts = new ArrayList<>();

    private String source;

    @JsonProperty("generatedAt")
    @JsonAlias({"generated_at", "generatedAt"})
    private String generatedAt;

    public InsightResponse() {
    }

    public InsightResponse(String headline, List<String> keyFindings, List<RecommendationItem> recommendations,
                           List<String> watchOuts, String source, String generatedAt) {
        this.headline = headline;
        this.keyFindings = keyFindings != null ? keyFindings : new ArrayList<>();
        this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
        this.watchOuts = watchOuts != null ? watchOuts : new ArrayList<>();
        this.source = source;
        this.generatedAt = generatedAt;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public List<String> getKeyFindings() {
        return keyFindings;
    }

    public void setKeyFindings(List<String> keyFindings) {
        this.keyFindings = keyFindings;
    }

    public List<RecommendationItem> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<RecommendationItem> recommendations) {
        this.recommendations = recommendations;
    }

    public List<String> getWatchOuts() {
        return watchOuts;
    }

    public void setWatchOuts(List<String> watchOuts) {
        this.watchOuts = watchOuts;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }
}
