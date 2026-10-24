package com.skillsense.survey.model.schema;

import java.util.ArrayList;
import java.util.List;

public class SurveyQuestionSchema {

    private Integer schemaVersion = 1;
    private List<QuestionDefinition> questions = new ArrayList<>();

    public SurveyQuestionSchema() {
    }

    public SurveyQuestionSchema(Integer schemaVersion, List<QuestionDefinition> questions) {
        this.schemaVersion = schemaVersion;
        this.questions = questions != null ? questions : new ArrayList<>();
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public List<QuestionDefinition> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionDefinition> questions) {
        this.questions = questions;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer schemaVersion = 1;
        private List<QuestionDefinition> questions = new ArrayList<>();

        public Builder schemaVersion(Integer schemaVersion) {
            this.schemaVersion = schemaVersion;
            return this;
        }

        public Builder questions(List<QuestionDefinition> questions) {
            this.questions = questions;
            return this;
        }

        public SurveyQuestionSchema build() {
            return new SurveyQuestionSchema(schemaVersion, questions);
        }
    }
}
