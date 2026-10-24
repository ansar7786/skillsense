package com.skillsense.term.dto;

import com.skillsense.term.AcademicTerm;

import java.time.LocalDate;

public class TermResponse {
    private Integer termId;
    private String label;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isCurrent;

    public TermResponse() {
    }

    public TermResponse(Integer termId, String label, LocalDate startDate, LocalDate endDate, Boolean isCurrent) {
        this.termId = termId;
        this.label = label;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isCurrent = isCurrent;
    }

    public static TermResponse fromEntity(AcademicTerm term) {
        if (term == null) return null;
        return TermResponse.builder()
                .termId(term.getTermId())
                .label(term.getLabel())
                .startDate(term.getStartDate())
                .endDate(term.getEndDate())
                .isCurrent(term.getIsCurrent())
                .build();
    }

    public Integer getTermId() {
        return termId;
    }

    public void setTermId(Integer termId) {
        this.termId = termId;
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
        private Integer termId;
        private String label;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean isCurrent;

        public Builder termId(Integer termId) {
            this.termId = termId;
            return this;
        }

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

        public TermResponse build() {
            return new TermResponse(termId, label, startDate, endDate, isCurrent);
        }
    }
}
