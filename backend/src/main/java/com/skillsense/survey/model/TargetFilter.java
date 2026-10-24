package com.skillsense.survey.model;

import java.util.ArrayList;
import java.util.List;

public class TargetFilter {

    private String scope = "ALL";
    private List<String> branches = new ArrayList<>();
    private List<Integer> years = new ArrayList<>();
    private List<String> sections = new ArrayList<>();

    public TargetFilter() {
    }

    public TargetFilter(String scope, List<String> branches, List<Integer> years, List<String> sections) {
        this.scope = scope != null ? scope : "ALL";
        this.branches = branches != null ? branches : new ArrayList<>();
        this.years = years != null ? years : new ArrayList<>();
        this.sections = sections != null ? sections : new ArrayList<>();
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public List<String> getBranches() {
        return branches;
    }

    public void setBranches(List<String> branches) {
        this.branches = branches;
    }

    public List<Integer> getYears() {
        return years;
    }

    public void setYears(List<Integer> years) {
        this.years = years;
    }

    public List<String> getSections() {
        return sections;
    }

    public void setSections(List<String> sections) {
        this.sections = sections;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String scope = "ALL";
        private List<String> branches = new ArrayList<>();
        private List<Integer> years = new ArrayList<>();
        private List<String> sections = new ArrayList<>();

        public Builder scope(String scope) {
            this.scope = scope;
            return this;
        }

        public Builder branches(List<String> branches) {
            this.branches = branches;
            return this;
        }

        public Builder years(List<Integer> years) {
            this.years = years;
            return this;
        }

        public Builder sections(List<String> sections) {
            this.sections = sections;
            return this;
        }

        public TargetFilter build() {
            return new TargetFilter(scope, branches, years, sections);
        }
    }
}
