package com.skillsense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsense.survey.model.QuestionType;
import com.skillsense.survey.model.schema.OptionDefinition;
import com.skillsense.survey.model.schema.QuestionDefinition;
import com.skillsense.survey.model.schema.RatingDefinition;
import com.skillsense.survey.model.schema.ScoringDefinition;
import com.skillsense.survey.model.schema.SurveyQuestionSchema;
import com.skillsense.survey.validation.QuestionSchemaValidator;
import com.skillsense.survey.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QuestionSchemaValidatorTest {

    private QuestionSchemaValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        validator = new QuestionSchemaValidator(objectMapper);
    }

    private QuestionDefinition createValidQuestion(String key) {
        return QuestionDefinition.builder()
                .key(key)
                .type(QuestionType.RATING)
                .prompt("Rate your proficiency in testing")
                .required(true)
                .rating(new RatingDefinition(1, 5, "Low", "High"))
                .build();
    }

    @Test
    public void testValidOneQuestionSchema() {
        SurveyQuestionSchema schema = SurveyQuestionSchema.builder()
                .schemaVersion(1)
                .questions(List.of(createValidQuestion("valid_q1")))
                .build();

        ValidationResult result = validator.validate(schema);
        assertTrue(result.isValid(), "1-question schema should be valid");
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testValidFortyQuestionsSchema() {
        List<QuestionDefinition> questions = new ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            questions.add(createValidQuestion("q_" + i));
        }

        SurveyQuestionSchema schema = SurveyQuestionSchema.builder()
                .schemaVersion(1)
                .questions(questions)
                .build();

        ValidationResult result = validator.validate(schema);
        assertTrue(result.isValid(), "40-question schema should be valid");
        assertTrue(result.getErrors().isEmpty());
        // 40 questions > 25, so warning must be present
        assertFalse(result.getWarnings().isEmpty(), "Warning should be exposed for >25 questions");
    }

    @Test
    public void testTwentySixQuestionsGeneratesWarning() {
        List<QuestionDefinition> questions = new ArrayList<>();
        for (int i = 1; i <= 26; i++) {
            questions.add(createValidQuestion("q_" + i));
        }

        SurveyQuestionSchema schema = SurveyQuestionSchema.builder()
                .schemaVersion(1)
                .questions(questions)
                .build();

        ValidationResult result = validator.validate(schema);
        assertTrue(result.isValid(), "26-question schema is still valid");
        assertFalse(result.getWarnings().isEmpty(), "Warning should be triggered for 26 questions");
        assertTrue(result.getWarnings().get(0).contains("over 25"));
    }

    @Test
    public void testInvalidQuestionCountZero() {
        SurveyQuestionSchema schema = SurveyQuestionSchema.builder()
                .schemaVersion(1)
                .questions(new ArrayList<>())
                .build();

        ValidationResult result = validator.validate(schema);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("at least 1 question")));
    }

    @Test
    public void testInvalidQuestionCountMoreThanForty() {
        List<QuestionDefinition> questions = new ArrayList<>();
        for (int i = 1; i <= 41; i++) {
            questions.add(createValidQuestion("q_" + i));
        }

        SurveyQuestionSchema schema = SurveyQuestionSchema.builder()
                .schemaVersion(1)
                .questions(questions)
                .build();

        ValidationResult result = validator.validate(schema);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("cannot contain more than 40 questions")));
    }

    @Test
    public void testInvalidKeyFormat() {
        QuestionDefinition q = createValidQuestion("InvalidKey!");
        SurveyQuestionSchema schema = SurveyQuestionSchema.builder()
                .schemaVersion(1)
                .questions(List.of(q))
                .build();

        ValidationResult result = validator.validate(schema);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("InvalidKey!") && e.contains("lowercase letters, digits, and underscores")));
    }

    @Test
    public void testKeyTooShort() {
        QuestionDefinition q = createValidQuestion("a");
        SurveyQuestionSchema schema = SurveyQuestionSchema.builder()
                .schemaVersion(1)
                .questions(List.of(q))
                .build();

        ValidationResult result = validator.validate(schema);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'a'") && e.contains("at least 2 characters long")));
    }

    @Test
    public void testKeyTooLong() {
        String longKey = "a".repeat(41);
        QuestionDefinition q = createValidQuestion(longKey);
        SurveyQuestionSchema schema = SurveyQuestionSchema.builder()
                .schemaVersion(1)
                .questions(List.of(q))
                .build();

        ValidationResult result = validator.validate(schema);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains(longKey) && e.contains("at most 40 characters long")));
    }

    @Test
    public void testDuplicateQuestionKeys() {
        QuestionDefinition q1 = createValidQuestion("duplicate_key");
        QuestionDefinition q2 = createValidQuestion("duplicate_key");
        SurveyQuestionSchema schema = SurveyQuestionSchema.builder()
                .schemaVersion(1)
                .questions(List.of(q1, q2))
                .build();

        ValidationResult result = validator.validate(schema);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Duplicate question key: 'duplicate_key'")));
    }

    @Test
    public void testInvalidQuestionTypeNull() {
        QuestionDefinition q = QuestionDefinition.builder()
                .key("null_type_q")
                .type(null)
                .prompt("Prompt for null type")
                .build();

        SurveyQuestionSchema schema = SurveyQuestionSchema.builder()
                .schemaVersion(1)
                .questions(List.of(q))
                .build();

        ValidationResult result = validator.validate(schema);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Question type is required for question 'null_type_q'")));
    }

    @Test
    public void testPromptTooShort() {
        QuestionDefinition q = QuestionDefinition.builder()
                .key("prompt_short")
                .type(QuestionType.YES_NO)
                .prompt("No")
                .build();

        SurveyQuestionSchema schema = SurveyQuestionSchema.builder()
                .schemaVersion(1)
                .questions(List.of(q))
                .build();

        ValidationResult result = validator.validate(schema);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("question 'prompt_short'") && e.contains("at least 3 characters")));
    }

    @Test
    public void testPromptTooLong() {
        String longPrompt = "P".repeat(301);
        QuestionDefinition q = QuestionDefinition.builder()
                .key("prompt_long")
                .type(QuestionType.YES_NO)
                .prompt(longPrompt)
                .build();

        SurveyQuestionSchema schema = SurveyQuestionSchema.builder()
                .schemaVersion(1)
                .questions(List.of(q))
                .build();

        ValidationResult result = validator.validate(schema);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("question 'prompt_long'") && e.contains("at most 300 characters")));
    }

    @Test
    public void testChoiceQuestionTooFewOptions() {
        QuestionDefinition q = QuestionDefinition.builder()
                .key("single_choice_q")
                .type(QuestionType.SINGLE_CHOICE)
                .prompt("Pick an option")
                .options(List.of(new OptionDefinition("opt1", "Option 1", null)))
                .build();

        SurveyQuestionSchema schema = SurveyQuestionSchema.builder()
                .schemaVersion(1)
                .questions(List.of(q))
                .build();

        ValidationResult result = validator.validate(schema);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.toLowerCase().contains("question 'single_choice_q'") && e.contains("at least 2 options")));
    }

    @Test
    public void testChoiceQuestionTooManyOptions() {
        List<OptionDefinition> options = new ArrayList<>();
        for (int i = 1; i <= 13; i++) {
            options.add(new OptionDefinition("opt_" + i, "Label " + i, null));
        }

        QuestionDefinition q = QuestionDefinition.builder()
                .key("multi_choice_q")
                .type(QuestionType.MULTI_CHOICE)
                .prompt("Pick from many")
                .options(options)
                .build();

        SurveyQuestionSchema schema = SurveyQuestionSchema.builder()
                .schemaVersion(1)
                .questions(List.of(q))
                .build();

        ValidationResult result = validator.validate(schema);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.toLowerCase().contains("question 'multi_choice_q'") && e.contains("cannot have more than 12 options")));
    }

    @Test
    public void testDuplicateOptionValues() {
        QuestionDefinition q = QuestionDefinition.builder()
                .key("choice_dup_opt")
                .type(QuestionType.SINGLE_CHOICE)
                .prompt("Select option")
                .options(Arrays.asList(
                        new OptionDefinition("same_val", "First Label", null),
                        new OptionDefinition("same_val", "Second Label", null)
                ))
                .build();

        SurveyQuestionSchema schema = SurveyQuestionSchema.builder()
                .schemaVersion(1)
                .questions(List.of(q))
                .build();

        ValidationResult result = validator.validate(schema);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Duplicate option value 'same_val' in question 'choice_dup_opt'")));
    }

    @Test
    public void testMissingOptionLabel() {
        QuestionDefinition q = QuestionDefinition.builder()
                .key("choice_missing_label")
                .type(QuestionType.SINGLE_CHOICE)
                .prompt("Select option")
                .options(Arrays.asList(
                        new OptionDefinition("val1", "", null),
                        new OptionDefinition("val2", "Valid Label", null)
                ))
                .build();

        SurveyQuestionSchema schema = SurveyQuestionSchema.builder()
                .schemaVersion(1)
                .questions(List.of(q))
                .build();

        ValidationResult result = validator.validate(schema);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Option label cannot be empty") && e.contains("choice_missing_label")));
    }

    @Test
    public void testInvalidOptionScore() {
        QuestionDefinition q = QuestionDefinition.builder()
                .key("choice_invalid_score")
                .type(QuestionType.SINGLE_CHOICE)
                .prompt("Select option")
                .options(Arrays.asList(
                        new OptionDefinition("val1", "Label 1", 120.0),
                        new OptionDefinition("val2", "Label 2", 0.0)
                ))
                .build();

        SurveyQuestionSchema schema = SurveyQuestionSchema.builder()
                .schemaVersion(1)
                .questions(List.of(q))
                .build();

        ValidationResult result = validator.validate(schema);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Option score must be between 0 and 100") && e.contains("choice_invalid_score")));
    }

    @Test
    public void testInvalidRatingMinMax() {
        QuestionDefinition q = QuestionDefinition.builder()
                .key("rating_invalid_range")
                .type(QuestionType.RATING)
                .prompt("Rate performance")
                .rating(new RatingDefinition(5, 5, "Min", "Max"))
                .build();

        SurveyQuestionSchema schema = SurveyQuestionSchema.builder()
                .schemaVersion(1)
                .questions(List.of(q))
                .build();

        ValidationResult result = validator.validate(schema);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Rating min (5) must be less than max (5)") && e.contains("rating_invalid_range")));
    }

    @Test
    public void testRatingMaxExceedsTen() {
        QuestionDefinition q = QuestionDefinition.builder()
                .key("rating_max_eleven")
                .type(QuestionType.RATING)
                .prompt("Rate performance")
                .rating(new RatingDefinition(1, 11, "Min", "Max"))
                .build();

        SurveyQuestionSchema schema = SurveyQuestionSchema.builder()
                .schemaVersion(1)
                .questions(List.of(q))
                .build();

        ValidationResult result = validator.validate(schema);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Rating max cannot exceed 10") && e.contains("rating_max_eleven")));
    }

    @Test
    public void testScoringOnUnsupportedQuestionType() {
        QuestionDefinition q = QuestionDefinition.builder()
                .key("text_with_score")
                .type(QuestionType.TEXT)
                .prompt("Share feedback")
                .scoring(new ScoringDefinition("comm", 1.0))
                .build();

        SurveyQuestionSchema schema = SurveyQuestionSchema.builder()
                .schemaVersion(1)
                .questions(List.of(q))
                .build();

        ValidationResult result = validator.validate(schema);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Scoring is not allowed for question type TEXT") && e.contains("text_with_score")));
    }

    @Test
    public void testInvalidScoringWeight() {
        QuestionDefinition q = QuestionDefinition.builder()
                .key("rating_bad_weight")
                .type(QuestionType.RATING)
                .prompt("Rate Java knowledge")
                .rating(new RatingDefinition(1, 5, "Beg", "Exp"))
                .scoring(new ScoringDefinition("java", 5.5))
                .build();

        SurveyQuestionSchema schema = SurveyQuestionSchema.builder()
                .schemaVersion(1)
                .questions(List.of(q))
                .build();

        ValidationResult result = validator.validate(schema);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Scoring weight must be between 0.5 and 5.0") && e.contains("rating_bad_weight")));
    }

    @Test
    public void testInvalidScoringSkillKey() {
        QuestionDefinition q = QuestionDefinition.builder()
                .key("rating_bad_skill")
                .type(QuestionType.RATING)
                .prompt("Rate Java knowledge")
                .rating(new RatingDefinition(1, 5, "Beg", "Exp"))
                .scoring(new ScoringDefinition("Java_Skill!", 2.0))
                .build();

        SurveyQuestionSchema schema = SurveyQuestionSchema.builder()
                .schemaVersion(1)
                .questions(List.of(q))
                .build();

        ValidationResult result = validator.validate(schema);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Scoring skill key 'Java_Skill!' must contain only lowercase") && e.contains("rating_bad_skill")));
    }
}
