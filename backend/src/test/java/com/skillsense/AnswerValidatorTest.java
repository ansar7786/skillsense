package com.skillsense;

import com.skillsense.survey.model.QuestionType;
import com.skillsense.survey.model.schema.OptionDefinition;
import com.skillsense.survey.model.schema.QuestionDefinition;
import com.skillsense.survey.model.schema.RatingDefinition;
import com.skillsense.survey.model.schema.SurveyQuestionSchema;
import com.skillsense.survey.validation.AnswerValidator;
import com.skillsense.survey.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AnswerValidatorTest {

    private AnswerValidator answerValidator;
    private SurveyQuestionSchema testSchema;

    @BeforeEach
    public void setUp() {
        answerValidator = new AnswerValidator();

        testSchema = SurveyQuestionSchema.builder()
                .schemaVersion(1)
                .questions(Arrays.asList(
                        QuestionDefinition.builder()
                                .key("q_single")
                                .type(QuestionType.SINGLE_CHOICE)
                                .prompt("Pick one")
                                .required(true)
                                .options(Arrays.asList(
                                        new OptionDefinition("opt1", "Option 1", null),
                                        new OptionDefinition("opt2", "Option 2", null)
                                ))
                                .build(),
                        QuestionDefinition.builder()
                                .key("q_multi")
                                .type(QuestionType.MULTI_CHOICE)
                                .prompt("Pick multiple")
                                .required(true)
                                .options(Arrays.asList(
                                        new OptionDefinition("m1", "M 1", null),
                                        new OptionDefinition("m2", "M 2", null)
                                ))
                                .build(),
                        QuestionDefinition.builder()
                                .key("q_rating")
                                .type(QuestionType.RATING)
                                .prompt("Rate 1 to 5")
                                .required(true)
                                .rating(new RatingDefinition(1, 5, "Min", "Max"))
                                .build(),
                        QuestionDefinition.builder()
                                .key("q_yesno")
                                .type(QuestionType.YES_NO)
                                .prompt("Yes or No")
                                .required(true)
                                .build(),
                        QuestionDefinition.builder()
                                .key("q_number")
                                .type(QuestionType.NUMBER)
                                .prompt("Enter number")
                                .required(true)
                                .min(0.0)
                                .max(100.0)
                                .build(),
                        QuestionDefinition.builder()
                                .key("q_text_opt")
                                .type(QuestionType.TEXT)
                                .prompt("Optional feedback")
                                .required(false)
                                .maxLength(20)
                                .build()
                ))
                .build();
    }

    @Test
    public void testValidAnswerMap() {
        Map<String, Object> answers = new HashMap<>();
        answers.put("q_single", "opt1");
        answers.put("q_multi", List.of("m1", "m2"));
        answers.put("q_rating", 4);
        answers.put("q_yesno", true);
        answers.put("q_number", 42.5);
        answers.put("q_text_opt", "Valid text");

        ValidationResult result = answerValidator.validateAnswers(testSchema, answers);
        assertTrue(result.isValid(), "Complete valid answers should pass");
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testOmittedOptionalAnswerAllowed() {
        Map<String, Object> answers = new HashMap<>();
        answers.put("q_single", "opt1");
        answers.put("q_multi", List.of("m1"));
        answers.put("q_rating", 3);
        answers.put("q_yesno", false);
        answers.put("q_number", 10.0);
        // q_text_opt is omitted

        ValidationResult result = answerValidator.validateAnswers(testSchema, answers);
        assertTrue(result.isValid(), "Omitted optional answer should be allowed");
    }

    @Test
    public void testMissingRequiredAnswer() {
        Map<String, Object> answers = new HashMap<>();
        answers.put("q_single", "opt1");
        // q_rating is missing
        answers.put("q_multi", List.of("m1"));
        answers.put("q_yesno", true);
        answers.put("q_number", 50.0);

        ValidationResult result = answerValidator.validateAnswers(testSchema, answers);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Missing required answer for question 'q_rating'")));
    }

    @Test
    public void testUnknownAnswerKey() {
        Map<String, Object> answers = new HashMap<>();
        answers.put("q_single", "opt1");
        answers.put("q_multi", List.of("m1"));
        answers.put("q_rating", 4);
        answers.put("q_yesno", true);
        answers.put("q_number", 42);
        answers.put("unknown_key_xyz", "some value");

        ValidationResult result = answerValidator.validateAnswers(testSchema, answers);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Unknown question key: 'unknown_key_xyz'")));
    }

    @Test
    public void testInvalidSingleChoiceValue() {
        Map<String, Object> answers = new HashMap<>();
        answers.put("q_single", "invalid_opt_value");
        answers.put("q_multi", List.of("m1"));
        answers.put("q_rating", 3);
        answers.put("q_yesno", true);
        answers.put("q_number", 10);

        ValidationResult result = answerValidator.validateAnswers(testSchema, answers);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Invalid option value 'invalid_opt_value' for question 'q_single'")));
    }

    @Test
    public void testInvalidMultiChoiceValue() {
        Map<String, Object> answers = new HashMap<>();
        answers.put("q_single", "opt1");
        answers.put("q_multi", List.of("m1", "bad_choice"));
        answers.put("q_rating", 3);
        answers.put("q_yesno", true);
        answers.put("q_number", 10);

        ValidationResult result = answerValidator.validateAnswers(testSchema, answers);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Invalid option value 'bad_choice' for question 'q_multi'")));
    }

    @Test
    public void testInvalidRatingValue() {
        Map<String, Object> answers = new HashMap<>();
        answers.put("q_single", "opt1");
        answers.put("q_multi", List.of("m1"));
        answers.put("q_rating", 7); // Range is 1..5
        answers.put("q_yesno", true);
        answers.put("q_number", 10);

        ValidationResult result = answerValidator.validateAnswers(testSchema, answers);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Rating value (7) outside allowed range [1..5]") && e.contains("q_rating")));
    }

    @Test
    public void testInvalidYesNoValue() {
        Map<String, Object> answers = new HashMap<>();
        answers.put("q_single", "opt1");
        answers.put("q_multi", List.of("m1"));
        answers.put("q_rating", 3);
        answers.put("q_yesno", "not_a_boolean");
        answers.put("q_number", 10);

        ValidationResult result = answerValidator.validateAnswers(testSchema, answers);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("must be a boolean (true/false) for question 'q_yesno'")));
    }

    @Test
    public void testInvalidNumberValue() {
        Map<String, Object> answers = new HashMap<>();
        answers.put("q_single", "opt1");
        answers.put("q_multi", List.of("m1"));
        answers.put("q_rating", 3);
        answers.put("q_yesno", true);
        answers.put("q_number", 150.0); // max is 100.0

        ValidationResult result = answerValidator.validateAnswers(testSchema, answers);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("is greater than max (100.0) for question 'q_number'")));
    }

    @Test
    public void testTextMaxLengthViolation() {
        Map<String, Object> answers = new HashMap<>();
        answers.put("q_single", "opt1");
        answers.put("q_multi", List.of("m1"));
        answers.put("q_rating", 3);
        answers.put("q_yesno", true);
        answers.put("q_number", 20.0);
        answers.put("q_text_opt", "This text is definitely way longer than 20 characters!");

        ValidationResult result = answerValidator.validateAnswers(testSchema, answers);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("exceeds maxLength of 20 for question 'q_text_opt'")));
    }
}
