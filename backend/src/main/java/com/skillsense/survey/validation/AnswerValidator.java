package com.skillsense.survey.validation;

import com.skillsense.common.error.ApiException;
import com.skillsense.common.error.ErrorCode;
import com.skillsense.survey.model.QuestionType;
import com.skillsense.survey.model.schema.OptionDefinition;
import com.skillsense.survey.model.schema.QuestionDefinition;
import com.skillsense.survey.model.schema.RatingDefinition;
import com.skillsense.survey.model.schema.SurveyQuestionSchema;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class AnswerValidator {

    public ValidationResult validateAnswers(SurveyQuestionSchema schema, Map<String, Object> answers) {
        ValidationResult result = new ValidationResult();

        if (schema == null || schema.getQuestions() == null) {
            result.addError("Question schema cannot be null");
            return result;
        }

        if (answers == null) {
            answers = new HashMap<>();
        }

        Map<String, QuestionDefinition> questionMap = new HashMap<>();
        for (QuestionDefinition q : schema.getQuestions()) {
            questionMap.put(q.getKey(), q);
        }

        // 1. Reject unknown answer keys
        for (String answerKey : answers.keySet()) {
            if (!questionMap.containsKey(answerKey)) {
                result.addError("Unknown question key: '" + answerKey + "'");
            }
        }

        // 2. Validate each question's answer
        for (QuestionDefinition q : schema.getQuestions()) {
            String key = q.getKey();
            Object value = answers.get(key);

            boolean isMissing = (value == null || (value instanceof String && ((String) value).trim().isEmpty()));

            if (isMissing) {
                if (Boolean.TRUE.equals(q.getRequired())) {
                    result.addError("Missing required answer for question '" + key + "'");
                }
                continue;
            }

            validateAnswerType(q, value, result);
        }

        return result;
    }

    public void validateAndThrow(SurveyQuestionSchema schema, Map<String, Object> answers) {
        ValidationResult result = validateAnswers(schema, answers);
        if (!result.isValid()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.VALIDATION_FAILED,
                    "Answer validation failed: " + String.join("; ", result.getErrors())
            );
        }
    }

    private void validateAnswerType(QuestionDefinition q, Object value, ValidationResult result) {
        String key = q.getKey();
        QuestionType type = q.getType();

        switch (type) {
            case SINGLE_CHOICE -> {
                if (!(value instanceof String)) {
                    result.addError("Answer for SINGLE_CHOICE question '" + key + "' must be a string");
                    return;
                }
                String valStr = (String) value;
                boolean matched = false;
                for (OptionDefinition opt : q.getOptions()) {
                    if (opt.getValue().equals(valStr)) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    result.addError("Invalid option value '" + valStr + "' for question '" + key + "'");
                }
            }

            case MULTI_CHOICE -> {
                if (!(value instanceof Collection<?> collection)) {
                    result.addError("Answer for MULTI_CHOICE question '" + key + "' must be an array of option values");
                    return;
                }
                if (Boolean.TRUE.equals(q.getRequired()) && collection.isEmpty()) {
                    result.addError("Missing required answer for question '" + key + "'");
                    return;
                }
                Set<String> validOptions = new HashSet<>();
                for (OptionDefinition opt : q.getOptions()) {
                    validOptions.add(opt.getValue());
                }
                for (Object item : collection) {
                    if (item == null || !validOptions.contains(item.toString())) {
                        result.addError("Invalid option value '" + item + "' for question '" + key + "'");
                    }
                }
            }

            case RATING -> {
                if (!(value instanceof Number num)) {
                    result.addError("Answer for RATING question '" + key + "' must be an integer");
                    return;
                }
                int ratingVal = num.intValue();
                RatingDefinition rating = q.getRating();
                if (rating != null) {
                    if (ratingVal < rating.getMin() || ratingVal > rating.getMax()) {
                        result.addError("Rating value (" + ratingVal + ") outside allowed range ["
                                + rating.getMin() + ".." + rating.getMax() + "] for question '" + key + "'");
                    }
                }
            }

            case YES_NO -> {
                if (!(value instanceof Boolean)) {
                    result.addError("Answer must be a boolean (true/false) for question '" + key + "'");
                }
            }

            case NUMBER -> {
                if (!(value instanceof Number num)) {
                    result.addError("Answer for NUMBER question '" + key + "' must be numeric");
                    return;
                }
                double numVal = num.doubleValue();
                if (q.getMin() != null && numVal < q.getMin()) {
                    result.addError("Number value (" + numVal + ") is less than min (" + q.getMin() + ") for question '" + key + "'");
                }
                if (q.getMax() != null && numVal > q.getMax()) {
                    result.addError("Number value (" + numVal + ") is greater than max (" + q.getMax() + ") for question '" + key + "'");
                }
            }

            case TEXT -> {
                if (!(value instanceof String strVal)) {
                    result.addError("Answer for TEXT question '" + key + "' must be a string");
                    return;
                }
                if (q.getMaxLength() != null && strVal.length() > q.getMaxLength()) {
                    result.addError("Text answer exceeds maxLength of " + q.getMaxLength() + " for question '" + key + "'");
                }
            }
        }
    }
}
