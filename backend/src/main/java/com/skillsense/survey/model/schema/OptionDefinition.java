package com.skillsense.survey.model.schema;

public class OptionDefinition {

    private String value;
    private String label;
    private Double score;

    public OptionDefinition() {
    }

    public OptionDefinition(String value, String label, Double score) {
        this.value = value;
        this.label = label;
        this.score = score;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String value;
        private String label;
        private Double score;

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder score(Double score) {
            this.score = score;
            return this;
        }

        public OptionDefinition build() {
            return new OptionDefinition(value, label, score);
        }
    }
}
