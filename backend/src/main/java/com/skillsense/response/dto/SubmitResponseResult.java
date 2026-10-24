package com.skillsense.response.dto;

public class SubmitResponseResult {

    private boolean submitted = true;

    public SubmitResponseResult() {
    }

    public SubmitResponseResult(boolean submitted) {
        this.submitted = submitted;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }
}
