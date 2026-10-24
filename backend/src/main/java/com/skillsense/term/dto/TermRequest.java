package com.skillsense.term.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class TermRequest {

    @NotBlank(message = "Label is required")
    @Size(max = 40, message = "Label cannot exceed 40 characters")
    private String label;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private Boolean isCurrent = false;

    public TermRequest() {
    }

    public TermRequest(String label, LocalDate startDate, LocalDate endDate, Boolean isCurrent) {
        this.label = label;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isCurrent = isCurrent != null ? isCurrent : false;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Boolean getIsCurrent() {
        return isCurrent;
    }

    public void setIsCurrent(Boolean isCurrent) {
        this.isCurrent = isCurrent;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String label;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean isCurrent = false;

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder startDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder endDate(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder isCurrent(Boolean isCurrent) {
            this.isCurrent = isCurrent;
            return this;
        }

        public TermRequest build() {
            return new TermRequest(label, startDate, endDate, isCurrent);
        }
    }
}
