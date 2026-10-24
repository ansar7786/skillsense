package com.skillsense.survey.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsense.common.error.ApiException;
import com.skillsense.common.error.ErrorCode;
import com.skillsense.survey.model.QuestionType;
import com.skillsense.survey.model.schema.OptionDefinition;
import com.skillsense.survey.model.schema.QuestionDefinition;
import com.skillsense.survey.model.schema.RatingDefinition;
import com.skillsense.survey.model.schema.ScoringDefinition;
import com.skillsense.survey.model.schema.SurveyQuestionSchema;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class QuestionSchemaValidator {

    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-z0-9_]+$");
    private final ObjectMapper objectMapper;

    public QuestionSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ValidationResult validate(SurveyQuestionSchema schema) {
        ValidationResult result = new ValidationResult();

        if (schema == null) {
            result.addError("Question schema cannot be null");
            return result;
        }

        if (schema.getSchemaVersion() == null || schema.getSchemaVersion() != 1) {
            result.addError("Unsupported schemaVersion: " + schema.getSchemaVersion() + ". Expected schemaVersion: 1");
        }

        List<QuestionDefinition> questions = schema.getQuestions();
        if (questions == null || questions.isEmpty()) {
            result.addError("Survey must contain at least 1 question");
            return result;
        }

        if (questions.size() > 40) {
            result.addError("Survey cannot contain more than 40 questions (found " + questions.size() + ")");
        } else if (questions.size() > 25) {
            result.addWarning("Survey contains " + questions.size() + " questions (over 25). Completion rate may drop.");
        }

        Set<String> seenKeys = new HashSet<>();

        for (int i = 0; i < questions.size(); i++) {
            QuestionDefinition q = questions.get(i);
            validateQuestion(q, i, seenKeys, result);
        }

        return result;
    }

    public SurveyQuestionSchema parseAndValidate(String json) {
        try {
            SurveyQuestionSchema schema = objectMapper.readValue(json, SurveyQuestionSchema.class);
            ValidationResult result = validate(schema);
            if (!result.isValid()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.VALIDATION_FAILED,
                        "Question schema validation failed: " + String.join("; ", result.getErrors())
                );
            }
            return schema;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.VALIDATION_FAILED,
                    "Malformed question schema JSON: " + e.getMessage()
            );
        }
    }

    private void validateQuestion(QuestionDefinition q, int index, Set<String> seenKeys, ValidationResult result) {
        String key = q.getKey();
        String questionRef = (key != null && !key.trim().isEmpty()) ? "question '" + key + "'" : "question at index " + index;

        // Key validation
        if (key == null || key.trim().isEmpty()) {
            result.addError("Question key is required for " + questionRef);
        } else {
            key = key.trim();
            if (key.length() < 2) {
                result.addError("Question key '" + key + "' must be at least 2 characters long");
            } else if (key.length() > 40) {
                result.addError("Question key '" + key + "' must be at most 40 characters long");
            }

            if (!KEY_PATTERN.matcher(key).matches()) {
                result.addError("Question key '" + key + "' must contain only lowercase letters, digits, and underscores");
            }

            if (seenKeys.contains(key)) {
                result.addError("Duplicate question key: '" + key + "'");
            } else {
                seenKeys.add(key);
            }
        }

        // Type validation
        QuestionType type = q.getType();
        if (type == null) {
            result.addError("Question type is required for " + questionRef);
        }

        // Prompt validation
        String prompt = q.getPrompt();
        if (prompt == null || prompt.trim().length() < 3) {
            result.addError("Prompt for " + questionRef + " must be at least 3 characters long");
        } else if (prompt.trim().length() > 300) {
            result.addError("Prompt for " + questionRef + " must be at most 300 characters long");
        }

        if (type == null) {
            return;
        }

        // Choice validation
        if (type == QuestionType.SINGLE_CHOICE || type == QuestionType.MULTI_CHOICE) {
            List<OptionDefinition> options = q.getOptions();
            if (options == null || options.size() < 2) {
                result.addError("Question '" + key + "' must have at least 2 options");
            } else if (options.size() > 12) {
                result.addError("Question '" + key + "' cannot have more than 12 options");
            } else {
                Set<String> seenOptionValues = new HashSet<>();
                for (OptionDefinition opt : options) {
                    if (opt.getValue() == null || opt.getValue().trim().isEmpty()) {
                        result.addError("Option value cannot be empty in " + questionRef);
                    } else {
                        String optVal = opt.getValue().trim();
                        if (seenOptionValues.contains(optVal)) {
                            result.addError("Duplicate option value '" + optVal + "' in " + questionRef);
                        } else {
                            seenOptionValues.add(optVal);
                        }
                    }

                    if (opt.getLabel() == null || opt.getLabel().trim().isEmpty()) {
                        result.addError("Option label cannot be empty for option '" + opt.getValue() + "' in " + questionRef);
                    }

                    if (opt.getScore() != null) {
                        if (opt.getScore() < 0 || opt.getScore() > 100) {
                            result.addError("Option score must be between 0 and 100 for option '" + opt.getValue() + "' in " + questionRef);
                        }
                    }
                }
            }
        }

        // RATING validation
        if (type == QuestionType.RATING) {
            RatingDefinition rating = q.getRating();
            if (rating == null) {
                result.addError("Rating configuration is required for " + questionRef);
            } else {
                if (rating.getMin() == null || rating.getMax() == null) {
                    result.addError("Rating min and max are required for " + questionRef);
                } else {
                    if (rating.getMin() >= rating.getMax()) {
                        result.addError("Rating min (" + rating.getMin() + ") must be less than max (" + rating.getMax() + ") for " + questionRef);
                    }
                    if (rating.getMax() > 10) {
                        result.addError("Rating max cannot exceed 10 for " + questionRef);
                    }
                }
            }
        }

        // NUMBER validation
        if (type == QuestionType.NUMBER) {
            if (q.getMin() != null && q.getMax() != null && q.getMin() >= q.getMax()) {
                result.addError("Number min (" + q.getMin() + ") must be less than max (" + q.getMax() + ") for " + questionRef);
            }
        }

        // TEXT validation
        if (type == QuestionType.TEXT) {
            if (q.getMaxLength() != null && q.getMaxLength() <= 0) {
                result.addError("Max length must be greater than 0 for " + questionRef);
            }
        }

        // SCORING validation
        ScoringDefinition scoring = q.getScoring();
        if (scoring != null) {
            if (type != QuestionType.RATING && type != QuestionType.SINGLE_CHOICE && type != QuestionType.YES_NO) {
                result.addError("Scoring is not allowed for question type " + type + " on " + questionRef);
            } else {
                Double weight = scoring.getWeight();
                if (weight == null || weight < 0.5 || weight > 5.0) {
                    result.addError("Scoring weight must be between 0.5 and 5.0 for " + questionRef);
                }

                String skill = scoring.getSkill();
                if (skill == null || skill.trim().isEmpty() || !KEY_PATTERN.matcher(skill.trim()).matches()) {
                    result.addError("Scoring skill key '" + skill + "' must contain only lowercase letters, digits, and underscores for " + questionRef);
                }
            }
        }
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
