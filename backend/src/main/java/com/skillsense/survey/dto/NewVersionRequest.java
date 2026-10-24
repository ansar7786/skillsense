package com.skillsense.survey.dto;

import jakarta.validation.constraints.NotNull;

public class NewVersionRequest {

    @NotNull(message = "Term ID is required")
    private Integer termId;

    public NewVersionRequest() {
    }

    public NewVersionRequest(Integer termId) {
        this.termId = termId;
    }

    public Integer getTermId() {
        return termId;
    }

    public void setTermId(Integer termId) {
        this.termId = termId;
    }
}
