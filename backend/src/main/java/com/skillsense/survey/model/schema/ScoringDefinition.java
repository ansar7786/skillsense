package com.skillsense.survey.model.schema;

public class ScoringDefinition {

    private String skill;
    private Double weight;

    public ScoringDefinition() {
    }

    public ScoringDefinition(String skill, Double weight) {
        this.skill = skill;
        this.weight = weight;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String skill;
        private Double weight;

        public Builder skill(String skill) {
            this.skill = skill;
            return this;
        }

        public Builder weight(Double weight) {
            this.weight = weight;
            return this;
        }

        public ScoringDefinition build() {
            return new ScoringDefinition(skill, weight);
        }
    }
}
