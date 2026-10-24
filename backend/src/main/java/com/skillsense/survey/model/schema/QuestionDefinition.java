package com.skillsense.survey.model.schema;

import com.skillsense.survey.model.QuestionType;

import java.util.ArrayList;
import java.util.List;

public class QuestionDefinition {

    private String key;
    private QuestionType type;
    private String section;
    private String prompt;
    private Boolean required = true;
    private List<OptionDefinition> options = new ArrayList<>();
    private RatingDefinition rating;
    private ScoringDefinition scoring;
    private Double min;
    private Double max;
    private Integer maxLength;

    public QuestionDefinition() {
    }

    public QuestionDefinition(String key, QuestionType type, String section, String prompt, Boolean required,
                              List<OptionDefinition> options, RatingDefinition rating, ScoringDefinition scoring,
                              Double min, Double max, Integer maxLength) {
        this.key = key;
        this.type = type;
        this.section = section;
        this.prompt = prompt;
        this.required = required != null ? required : true;
        this.options = options != null ? options : new ArrayList<>();
        this.rating = rating;
        this.scoring = scoring;
        this.min = min;
        this.max = max;
        this.maxLength = maxLength;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public QuestionType getType() {
        return type;
    }

    public void setType(QuestionType type) {
        this.type = type;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Boolean getRequired() {
        return required != null ? required : true;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public List<OptionDefinition> getOptions() {
        return options;
    }

    public void setOptions(List<OptionDefinition> options) {
        this.options = options;
    }

    public RatingDefinition getRating() {
        return rating;
    }

    public void setRating(RatingDefinition rating) {
        this.rating = rating;
    }

    public ScoringDefinition getScoring() {
        return scoring;
    }

    public void setScoring(ScoringDefinition scoring) {
        this.scoring = scoring;
    }

    public Double getMin() {
        return min;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String key;
        private QuestionType type;
        private String section;
        private String prompt;
        private Boolean required = true;
        private List<OptionDefinition> options = new ArrayList<>();
        private RatingDefinition rating;
        private ScoringDefinition scoring;
        private Double min;
        private Double max;
        private Integer maxLength;

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder type(QuestionType type) {
            this.type = type;
            return this;
        }

        public Builder section(String section) {
            this.section = section;
            return this;
        }

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder required(Boolean required) {
            this.required = required;
            return this;
        }

        public Builder options(List<OptionDefinition> options) {
            this.options = options;
            return this;
        }

        public Builder rating(RatingDefinition rating) {
            this.rating = rating;
            return this;
        }

        public Builder scoring(ScoringDefinition scoring) {
            this.scoring = scoring;
            return this;
        }

        public Builder min(Double min) {
            this.min = min;
            return this;
        }

        public Builder max(Double max) {
            this.max = max;
            return this;
        }

        public Builder maxLength(Integer maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        public QuestionDefinition build() {
            return new QuestionDefinition(key, type, section, prompt, required, options, rating, scoring, min, max, maxLength);
        }
    }
}
