package com.skillsense.survey.model.schema;

public class RatingDefinition {

    private Integer min;
    private Integer max;
    private String minLabel;
    private String maxLabel;

    public RatingDefinition() {
    }

    public RatingDefinition(Integer min, Integer max, String minLabel, String maxLabel) {
        this.min = min;
        this.max = max;
        this.minLabel = minLabel;
        this.maxLabel = maxLabel;
    }

    public Integer getMin() {
        return min;
    }

    public void setMin(Integer min) {
        this.min = min;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }

    public String getMinLabel() {
        return minLabel;
    }

    public void setMinLabel(String minLabel) {
        this.minLabel = minLabel;
    }

    public String getMaxLabel() {
        return maxLabel;
    }

    public void setMaxLabel(String maxLabel) {
        this.maxLabel = maxLabel;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer min;
        private Integer max;
        private String minLabel;
        private String maxLabel;

        public Builder min(Integer min) {
            this.min = min;
            return this;
        }

        public Builder max(Integer max) {
            this.max = max;
            return this;
        }

        public Builder minLabel(String minLabel) {
            this.minLabel = minLabel;
            return this;
        }

        public Builder maxLabel(String maxLabel) {
            this.maxLabel = maxLabel;
            return this;
        }

        public RatingDefinition build() {
            return new RatingDefinition(min, max, minLabel, maxLabel);
        }
    }
}
