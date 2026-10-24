package com.skillsense.response.dto;

import jakarta.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.Map;

public class SubmitResponseRequest {

    @NotNull(message = "Answers are required")
    private Map<String, Object> answers = new HashMap<>();

    public SubmitResponseRequest() {
    }

    public SubmitResponseRequest(Map<String, Object> answers) {
        this.answers = answers != null ? answers : new HashMap<>();
    }

    public Map<String, Object> getAnswers() {
        return answers;
    }

    public void setAnswers(Map<String, Object> answers) {
        this.answers = answers;
    }
}
